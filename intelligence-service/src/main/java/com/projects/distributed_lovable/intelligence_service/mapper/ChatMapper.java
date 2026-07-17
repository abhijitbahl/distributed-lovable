package com.projects.distributed_lovable.intelligence_service.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.projects.distributed_lovable.intelligence_service.dto.ChatResponse;
import com.projects.distributed_lovable.intelligence_service.entity.ChatMessage;


@Mapper(componentModel = "spring")
public interface ChatMapper {
    List<ChatResponse> fromListOfChatMessage(List<ChatMessage> chatMessageList);
}
