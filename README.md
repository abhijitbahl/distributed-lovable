# Lovable Clone — Distributed Application

An AI-powered app builder (a "Lovable clone"): users describe an app in chat, an LLM
generates/edits the project's files, and the result is instantly previewable and
deployable — all running as a set of independently deployed Spring Boot
microservices on Kubernetes.

Live at **https://lovableclone.in**.

## Repositories

| Repo | Contents |
|---|---|
| `distributed-lovable` (this repo) | All backend microservices, shared library, and Kubernetes/infra manifests |
| [`Front_End_Project_Companion`](https://github.com/abhijitbahl/Front_End_Project_Companion) | React + Vite frontend (chat UI, code editor, live preview) |

## Architecture overview

```
                                   ┌─────────────────────┐
                                   │   Browser (React)   │
                                   └──────────┬───────────┘
                                              │ HTTPS
                                   ┌──────────▼───────────┐
                                   │   nginx Ingress       │  lovableclone.in (frontend)
                                   │   (TLS termination)    │  api.lovableclone.in (API)
                                   └──────────┬───────────┘  *.previews.lovableclone.in (live previews)
                                              │
                                   ┌──────────▼───────────┐
                                   │      api-gateway      │  JWT auth + routing + CORS
                                   └──────────┬───────────┘
                     ┌────────────────┬───────┴───────┬────────────────┐
                     ▼                ▼               ▼                ▼
              account-service  workspace-service  intelligence-service  (previews via
              (auth, billing)  (projects, files,  (AI chat, code        lovable-me-proxy)
                                members, deploy)   generation)
                     │                │               │
                     └────────┬───────┴───────┬───────┘
                              ▼               ▼
                        Postgres/pgvector   Kafka (async events)
                        Redis · MinIO
```

`config-service` and `discovery-service` (Spring Cloud Config + Eureka) sit
alongside these and are used by every service for centralized configuration and
service discovery.

## Services

| Service | Responsibility | Key tech |
|---|---|---|
| **api-gateway** | Single entry point for all API traffic. Validates JWTs, enforces CORS, routes to the right downstream service. | Spring Cloud Gateway (reactive), WebFlux |
| **account-service** | Signup/login (JWT issuance), user profiles, Stripe billing (checkout, customer portal, webhook-driven subscription state, usage limits). | Spring Security, Stripe SDK |
| **workspace-service** | Owns projects: file storage/tree, project members & invites (with email notifications), live preview deployment, Kubernetes pod orchestration for previews. | Spring Data JPA, Fabric8 Kubernetes client, MinIO, Redis, `spring-boot-starter-mail` |
| **intelligence-service** | AI chat: streams LLM responses (SSE) that generate/edit project files, backed by pgvector for retrieval. | Spring AI (OpenAI-compatible), Kafka |
| **config-service** | Spring Cloud Config Server — serves per-service `application.yml` from a separate private config repo (`lovable-config-server`), so config/secrets never live in application source. | Spring Cloud Config |
| **discovery-service** | Eureka service registry for local/dev service discovery. | Netflix Eureka |
| **common-lib** | Shared DTOs, Kafka event contracts, JWT/security utilities used by every service. | — |
| `k8s/proxy` (lovable-me-proxy) | Node.js reverse proxy that routes `*.previews.lovableclone.in` traffic to the right in-cluster preview pod, using Redis to look up hostname → pod. | Node, `http-proxy`, ioredis |

## Cross-service communication

- **Synchronous**: OpenFeign clients for direct request/response calls (e.g.
  `workspace-service` → `account-service` to look up a user by email/id for
  invites and member listings).
- **Asynchronous (Kafka)**: for events that shouldn't block the caller —
  e.g. `account-service` publishes `UserSignedUpEvent` on signup, which
  `workspace-service` consumes to auto-resolve any pending project invites for
  that email into real memberships (see [Saga pattern](#saga-pattern-ai-generated-file-writes)
  below for the file-write flow specifically).

## Saga pattern: AI-generated file writes

Persisting a file the LLM generates spans two services with two separate
databases (`intelligence-service`'s `chat_events` and `workspace-service`'s
file storage), so it can't be a single ACID transaction. This is handled as a
**choreographed saga** over Kafka — each service reacts to events and commits
its own local transaction, rather than a central coordinator driving the
whole flow:

1. **`intelligence-service`** — for each file-editing chat event the LLM
   produces, it generates a `sagaId`, stores the `ChatEvent` locally with
   status `PENDING`, and publishes a `FileStoreRequestEvent` (topic
   `file-storage-request-event`, keyed by `project-{projectId}`).
2. **`workspace-service`** (`FileStorageConsumer`) — consumes the request,
   checks a `ProcessedEvent` table keyed by `sagaId` for idempotency (so a
   redelivered message doesn't write the file twice), saves the file, records
   the `sagaId` as processed, and publishes a `FileStoreResponseEvent` (topic
   `file-store-responses`) reporting success or failure.
3. **`intelligence-service`** (`IntelligenceSageResponseHandler`) — consumes
   the response, looks up the `ChatEvent` by `sagaId`, and (if not already
   handled) marks it `CONFIRMED` or `FAILED`.

The `sagaId` is the idempotency key on both sides, which is what makes the
flow safe to retry: Kafka's at-least-once delivery means either event could
arrive more than once, and both consumers check "have I already handled this
sagaId?" before acting.

## Data & infrastructure

- **Postgres (pgvector)** — relational data per service (separate databases),
  plus vector embeddings for `intelligence-service`'s retrieval.
- **Redis** — preview hostname routing (`lovable-me-proxy`) and other caching.
- **MinIO** — object storage for project files.
- **Kafka** — async cross-service events.
- **GKE (Autopilot)** — the cluster all of this runs on.
- **nginx-ingress + cert-manager** — TLS via Let's Encrypt. The main domain and
  `api.` subdomain use HTTP-01; the `*.previews.` wildcard uses DNS-01 via a
  GoDaddy webhook, since wildcard certs require proving DNS control rather
  than serving an HTTP challenge.

## Deployment

Each service has its own GitHub Actions workflow (`.github/workflows/deploy-*.yaml`)
that triggers on pushes touching that service's path: it builds a Docker
image tagged with the commit SHA (via Jib), pushes it to Docker Hub, and runs
`kubectl set image` against its GKE deployment. Building with the immutable
commit SHA (rather than `:latest`) avoids GKE's Docker Hub pull-through cache
serving a stale image after a new push.

The frontend has its own equivalent workflow in its separate repository.

## Local development

Each service reads shared configuration from `config-service`, which in turn
pulls from the private `lovable-config-server` repo — see that repo for local
profile values (ports, local Postgres/Redis/MinIO connection strings, etc.).
`discovery-service` (Eureka) and `config-service` need to be running before
the other services will start cleanly locally.
