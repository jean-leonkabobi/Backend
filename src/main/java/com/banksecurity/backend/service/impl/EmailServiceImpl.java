package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.integration.email.EmailClient;
import com.banksecurity.backend.integration.email.EmailMessage;
import com.banksecurity.backend.model.Alert;
import com.banksecurity.backend.model.User;
import com.banksecurity.backend.service.EmailService;
import com.banksecurity.backend.util.AsyncUtils;
import com.banksecurity.backend.util.Constants;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final EmailClient emailClient;

    @Resource(name = "notificationExecutor")
    private Executor notificationExecutor;

    @Override
    public void sendAlertEmail(Alert alert, byte[] imageData) {
        if (!emailClient.isConfigured()) {
            log.warn("Email non configuré. Impossible d'envoyer l'alerte.");
            return;
        }

        // ✅ Utilisation de Constants.EMAIL_ALERT_SUBJECT
        String subject = Constants.EMAIL_ALERT_SUBJECT + alert.getSeverity();
        String content = buildAlertEmailContent(alert);

        EmailMessage message = EmailMessage.builder()
                // ✅ Utilisation de Constants.EMAIL_FROM
                .from(Constants.EMAIL_FROM)
                .to(new String[]{"security-team@banksecurity.com"})
                .subject(subject)
                .content(content)
                .html(true)
                .build();

        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> emailClient.sendHtmlEmail(message),
                notificationExecutor,
                "Envoi email alerte " + alert.getId()
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de l'envoi de l'email d'alerte: {}", e.getMessage());
            return null;
        });

        log.info("Email d'alerte envoyé (async): {} - {}", alert.getType(), alert.getSeverity());
    }

    @Override
    public void sendNotificationEmail(String to, String subject, String content) {
        if (!emailClient.isConfigured()) {
            log.warn("Email non configuré. Impossible d'envoyer la notification.");
            return;
        }

        EmailMessage message = EmailMessage.builder()
                .from(Constants.EMAIL_FROM)
                .to(new String[]{to})
                .subject(subject)
                .content(content)
                .html(false)
                .build();

        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> emailClient.sendSimpleEmail(message),
                notificationExecutor,
                "Envoi notification à " + to
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de l'envoi de la notification: {}", e.getMessage());
            return null;
        });

        log.info("Email de notification envoyé (async) à: {}", to);
    }

    @Override
    public void sendWelcomeEmail(User user) {
        String subject = "Bienvenue sur " + Constants.APP_NAME;
        String content = String.format(
                "Bonjour %s %s,\n\n" +
                        "Votre compte a été créé avec succès.\n" +
                        "Email: %s\n" +
                        "Rôle: %s\n\n" +
                        "Cordialement,\n" +
                        "L'équipe de sécurité",
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().getDescription()
        );

        sendNotificationEmail(user.getEmail(), subject, content);
    }

    @Override
    public void sendPasswordResetEmail(User user, String resetToken) {
        String subject = "Réinitialisation de votre mot de passe";
        String content = String.format(
                "Bonjour %s %s,\n\n" +
                        "Vous avez demandé la réinitialisation de votre mot de passe.\n" +
                        "Token de réinitialisation: %s\n\n" +
                        "Ce token expire dans 24 heures.\n\n" +
                        "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe de sécurité",
                user.getFirstName(),
                user.getLastName(),
                resetToken
        );

        sendNotificationEmail(user.getEmail(), subject, content);
    }

    @Override
    public void sendAccountLockedEmail(User user) {
        String subject = "Compte verrouillé - " + Constants.APP_NAME;
        String content = String.format(
                "Bonjour %s %s,\n\n" +
                        "Votre compte a été verrouillé suite à plusieurs tentatives de connexion échouées.\n\n" +
                        "Veuillez contacter votre administrateur pour déverrouiller votre compte.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe de sécurité",
                user.getFirstName(),
                user.getLastName()
        );

        sendNotificationEmail(user.getEmail(), subject, content);
    }

    @Override
    public void sendAccountUnlockedEmail(User user) {
        String subject = "Compte déverrouillé - " + Constants.APP_NAME;
        String content = String.format(
                "Bonjour %s %s,\n\n" +
                        "Votre compte a été déverrouillé. Vous pouvez maintenant vous connecter.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe de sécurité",
                user.getFirstName(),
                user.getLastName()
        );

        sendNotificationEmail(user.getEmail(), subject, content);
    }

    @Override
    public void sendDailyReportEmail(String to, String reportContent) {
        String subject = "Rapport quotidien de sécurité";
        sendNotificationEmail(to, subject, reportContent);
    }

    @Override
    public boolean isEmailConfigured() {
        return emailClient.isConfigured();
    }

    private String buildAlertEmailContent(Alert alert) {
        StringBuilder content = new StringBuilder();
        content.append("<html><body>");
        content.append("<h2>Alerte de sécurité</h2>");
        content.append("<p><strong>Type:</strong> ").append(alert.getType()).append("</p>");
        content.append("<p><strong>Sévérité:</strong> ").append(alert.getSeverity()).append("</p>");
        content.append("<p><strong>Statut:</strong> ").append(alert.getStatus()).append("</p>");
        content.append("<p><strong>Date:</strong> ").append(alert.getCreatedAt()).append("</p>");

        if (alert.getDescription() != null) {
            content.append("<p><strong>Description:</strong> ").append(alert.getDescription()).append("</p>");
        }

        if (alert.getDetectionConfidence() != null) {
            content.append("<p><strong>Confiance:</strong> ").append(alert.getDetectionConfidence()).append("</p>");
        }

        content.append("</body></html>");
        return content.toString();
    }
}