package com.projects.distributed_lovable.workspace_service.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.projects.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.projects.distributed_lovable.workspace_service.entity.Project;
import com.projects.distributed_lovable.workspace_service.entity.ProjectFile;
import com.projects.distributed_lovable.workspace_service.repository.ProjectFileRepository;
import com.projects.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.projects.distributed_lovable.workspace_service.service.ProjectTemplateService;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectTemplateServiceImpl implements ProjectTemplateService {

    private final MinioClient minioClient;
    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;

    private static final String TEMPLATE_BUCKET = "starter-projects";
    private static final String TARGET_BUCKET = "projects";
    private static final String TEMPLATE_NAME = "react-vite-tailwind-daisyui-starter";

    @Override
    public void initializeProjectFromTemplate(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(TEMPLATE_BUCKET)
                            .prefix(TEMPLATE_NAME + "/")
                            .recursive(true)
                            .build());

            List<ProjectFile> filesToSave = new ArrayList<>();// for metadata in postgresql

            for (Result<Item> result : results) {
                Item item = result.get();
                String sourceKey = item.objectName();
                String cleanPath = sourceKey.replaceFirst(TEMPLATE_NAME + "/", "");
                String destinationKey = projectId + "/" + cleanPath;

                if (!cleanPath.isEmpty()) {
                    String targetObjectName = destinationKey;

                    // Copy the object from the template bucket to the target bucket
                    minioClient.copyObject(
                            CopyObjectArgs.builder()
                                    .source(CopySource.builder()
                                            .bucket(TEMPLATE_BUCKET)
                                            .object(sourceKey)
                                            .build())
                                    .bucket(TARGET_BUCKET)
                                    .object(targetObjectName)
                                    .build());

                    // Create a ProjectFile entity and add it to the list
                    ProjectFile projectFile = ProjectFile.builder()
                            .path(cleanPath)
                            .project(project)
                            .minioObjectKey(destinationKey)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    filesToSave.add(projectFile);
                }
            }
            // Save all ProjectFile entities to the database
            projectFileRepository.saveAll(filesToSave);
        } catch (Exception e) {
            log.error("Error initializing project from template", e);
            throw new RuntimeException("Error initializing project from template", e);
        }
    }
}
