# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Backend for an AI app-builder ("Lovable clone"): a set of independently
deployed Spring Boot microservices on GKE. See `README.md` for the full
architecture diagram, service responsibilities, and the Saga pattern used for
AI-generated file writes — this file focuses on how to actually work in the
repo day to day. The frontend lives in a separate repo
([`Front_End_Project_Companion`](https://github.com/abhijitbahl/Front_End_Project_Companion)),
not here.

Services: `account-service`, `workspace-service`, `intelligence-service`,
`api-gateway`, `config-service`, `discovery-service`, plus `common-lib`
(shared DTOs/events/security, consumed by every service).

## Build & test

Each service is a separate Maven module with its own `mvnw`. **Always build
and install `common-lib` first** — every other service depends on it as a
local Maven artifact (`com.projects.distributed-lovable:common-lib:1.0.0`),
and changes to it won't be picked up by other services until reinstalled:

```bash
cd common-lib && ./mvnw -q install -DskipTests
```

Then, per service:

```bash
cd <service> && ./mvnw compile         # compile-check
cd <service> && ./mvnw test            # run tests (currently just placeholder contextLoads tests)
cd <service> && ./mvnw spring-boot:run # run locally
```

Local runs require `config-service` and `discovery-service` (Eureka)
running first — every service's local `application.yaml` does nothing but
import config from `configserver:${CONFIG_SERVER_URL:http://localhost:8888}`,
so without config-service up, nothing else will start. The actual per-service
config (DB/Redis/Kafka/MinIO connection strings, ports, JWT secret, etc.) is
**not in this repo** — `config-service` pulls it from a separate private repo
(`lovable-config-server`) at startup. You also need Postgres, Redis, Kafka,
and MinIO running locally (no docker-compose file is checked in; provision
these yourself matching the ports in that config repo).

Local ports: `discovery-service` 8761, `config-service` 8888, `api-gateway`
8080, `account-service` 9050 (context-path `/account`), `workspace-service`
9020 (context-path `/workspace`), `intelligence-service` 9030.

## Deployment

Each service has its own GitHub Actions workflow
(`.github/workflows/deploy-<service>.yaml`) triggered by pushes touching that
service's path (or `k8s/**` where relevant). It builds/pushes a Docker image
via Jib tagged with the **commit SHA**, then runs `kubectl set image` against
the GKE deployment.

**Never deploy with a `:latest` tag manually via `docker build`/`kubectl set
image` on this cluster** — GKE's Docker Hub pull-through cache
(`mirror.gcr.io`) serves a stale cached manifest for `:latest` for a while
after a push, so a fresh push often doesn't actually reach the pod. Always
use a unique tag (the CI's SHA-tagging exists specifically to avoid this).
`k8s/services/*.yaml` deployment manifests keep `:latest` as a placeholder
value only — the real running tag is whatever CI last set via `kubectl set
image`, so `kubectl apply -f` on those files won't change the image, only
other fields (env vars, resources, etc.).

`k8s/infra/*.yaml` and `k8s/stateful/*.yaml` changes aren't wired to any CI
workflow — apply them manually with `kubectl apply -f`.

## GKE Autopilot gotchas

This cluster is **GKE Autopilot**, which blocks writes to `kube-system` (a
"managed" namespace). Any component that defaults to leader-electing or
otherwise writing there (cert-manager's controller/cainjector did, out of the
box) needs `--leader-election-namespace` repointed at its own namespace, plus
a matching Role/RoleBinding granting `leases` permissions there — see
`k8s/infra/cert-manager-autopilot-fix.yaml`.

NetworkPolicies in `lovable-core` (`k8s/infra/core-network-policies.yaml`)
are allowlist-based per `app` label — a new workload that needs ingress from
`ingress-nginx` (e.g. a cert-manager ACME solver pod) won't get it
automatically just by living in the namespace. See
`k8s/infra/allow-acme-solver.yaml` for the pattern.

## Auth conventions

- User-facing endpoints require a JWT (validated by each service's
  `JwtAuthFilter`/security config).
- Internal service-to-service endpoints live under `/internal/v1/**` and must
  be explicitly permitted without auth in that service's security config
  (`.requestMatchers("/internal/**").permitAll()`) — this is easy to forget
  when adding a new internal endpoint, and the failure mode is a silent 401
  on the calling service, not an obvious error at the call site.
- `@PreAuthorize("@security.canX(...)")` checks in `workspace-service`
  resolve against `ProjectRole`'s permission set (`common-lib`'s
  `ProjectRole` enum) — e.g. only `OWNER` has `MANAGE_MEMBERS`, so `EDITOR`
  accounts get a 403 inviting/removing members by design, not by bug.

## TLS

`lovableclone.in`, `www.`, and `api.` get certs via HTTP-01
(`k8s/infra/cluster-issuer.yaml` + `ingress.yaml`). The `*.previews.` wildcard
needs DNS-01 instead (wildcards can't use HTTP-01), handled via a GoDaddy
webhook solver, selector-scoped to that host in the same `ClusterIssuer`. If
you ever touch `ingress.yaml`'s `ssl-redirect` annotation, know that turning
it on breaks ACME HTTP-01 renewal (nginx redirects the challenge path to
https before a valid cert exists).
