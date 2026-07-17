package com.projects.distributed_lovable.workspace_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projects.distributed_lovable.common_lib.dto.FileTreeDto;
import com.projects.distributed_lovable.workspace_service.service.ProjectFileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/files")
@RequiredArgsConstructor
public class FileController {
    private final ProjectFileService fileService;

    @GetMapping()
    public ResponseEntity<FileTreeDto> getFileTree(@PathVariable Long projectId) {
        return ResponseEntity.ok(fileService.getFileTree(projectId));
    }

    @GetMapping("/content")
    public ResponseEntity<String> getFile(@PathVariable Long projectId, @RequestParam String path) {
        return ResponseEntity.ok(fileService.getFileContent(projectId, path));
    }

}
