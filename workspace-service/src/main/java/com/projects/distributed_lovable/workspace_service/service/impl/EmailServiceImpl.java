package com.projects.distributed_lovable.workspace_service.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.projects.distributed_lovable.workspace_service.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    @Async
    public void sendProjectInviteEmail(String toEmail, String projectName, boolean hasAccount) {
        String actionUrl = hasAccount ? frontendUrl + "/login" : frontendUrl + "/signup";
        String actionLabel = hasAccount ? "Log in" : "Sign up";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("You've been invited to a project on Lovable Clone");
        message.setText("""
                You've been invited to collaborate on "%s" on Lovable Clone.

                %s here to view the project: %s
                """.formatted(projectName, actionLabel, actionUrl));

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send invite email to {}", toEmail, e);
        }
    }
}
