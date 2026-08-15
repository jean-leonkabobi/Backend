package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.integration.email.EmailClient;
import com.banksecurity.backend.integration.email.EmailMessage;
import com.banksecurity.backend.model.Alert;
import com.banksecurity.backend.model.User;
import com.banksecurity.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final EmailClient emailClient;

    @Override
    @Async("notificationExecutor")
    public void sendAlertEmail(Alert alert, byte[] imageData) {
        if (!emailClient.isConfigured()) {
            log.warn("Email non configuré. Impossible d'envoyer l'alerte.");
            return;
        }

        String subject = "🚨 Alerte de sécurité - " + alert.getSeverity();
        String content = buildAlertEmailContent(alert);

        EmailMessage message = EmailMessage.builder()
                .to(new String[]{"security-team@banksecurity.com"})
                .subject(subject)
                .content(content)
                .html(true)
                .build();

        emailClient.sendHtmlEmail(message);
        log.info("Email d'alerte envoyé: {} - {}", alert.getType(), alert.getSeverity());
    }

    @Override
    @Async("notificationExecutor")
    public void sendNotificationEmail(String to, String subject, String content) {
        if (!emailClient.isConfigured()) {
            log.warn("Email non configuré. Impossible d'envoyer la notification.");
            return;
        }

        EmailMessage message = EmailMessage.builder()
                .to(new String[]{to})
                .subject(subject)
                .content(content)
                .html(false)
                .build();

        emailClient.sendSimpleEmail(message);
        log.info("Email de notification envoyé à: {}", to);
    }

    @Override
    @Async("notificationExecutor")
    public void sendWelcomeEmail(User user) {
        String subject = "Bienvenue sur Bank Security System";
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
    @Async("notificationExecutor")
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
    @Async("notificationExecutor")
    public void sendAccountLockedEmail(User user) {
        String subject = "Compte verrouillé - Bank Security System";
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
    @Async("notificationExecutor")
    public void sendAccountUnlockedEmail(User user) {
        String subject = "Compte déverrouillé - Bank Security System";
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
    @Async("notificationExecutor")
    public void sendDailyReportEmail(String to, String reportContent) {
        String subject = "Rapport quotidien de sécurité";
        sendNotificationEmail(to, subject, reportContent);
    }

    @Override
    public boolean isEmailConfigured() {
        return emailClient.isConfigured();
    }

    /**
     * Construit le contenu HTML de l'email d'alerte
     */
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