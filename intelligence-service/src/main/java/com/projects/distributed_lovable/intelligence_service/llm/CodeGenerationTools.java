package com.projects.distributed_lovable.intelligence_service.llm;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import com.projects.distributed_lovable.intelligence_service.client.WorkspaceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class CodeGenerationTools {

    private final Long projectId;
    private final WorkspaceClient workspaceClient;

    @Tool(name = "read_files", description = "Read the content of the files. Only input the file name present inside the FILE_TREE. DO NOT input any path which is not present under the FILE_TREE.")
    public List<String> readFiles(
            @ToolParam(description = "List of relative paths(e.g., ['src/app.tsx'])") List<String> paths) {

        List<String> result = new ArrayList<>();

        for (String path : paths) {
            String cleanPath = path.startsWith("/") ? path.substring(1) : path;

            log.info("Requested file {}", cleanPath);

            String fileContent = workspaceClient.getFileContent(projectId, cleanPath);
            result.add(String.format("---START OF FILE %s ---\n%s\n---END OF FILE ---", cleanPath, fileContent));
        }
        return result;
    }
}
