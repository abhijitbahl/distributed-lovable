package com.projects.distributed_lovable.workspace_service.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.projects.distributed_lovable.common_lib.dto.FileNode;
import com.projects.distributed_lovable.workspace_service.entity.ProjectFile;

@Mapper(componentModel = "spring")
public interface ProjectFileMapper {
    List<FileNode> toFileNodeList(List<ProjectFile> projectFileList);
}
