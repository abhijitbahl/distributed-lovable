package com.projects.distributed_lovable.workspace_service.service;

public interface EmailService {

    void sendProjectInviteEmail(String toEmail, String projectName, boolean hasAccount);
}
