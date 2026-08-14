package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.model.Alert;
import com.banksecurity.backend.model.User;
import com.banksecurity.backend.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Override
    @Async("notificationExecutor")
    public void sendAlertEmail(Alert alert, byte[] imageData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo("security-team@banksecurity.com"); // À configurer
            helper.setSubject("🚨 Alerte de sécurité - " + alert.getSeverity());

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

            helper.setText(content.toString(), true);

            // Ajouter l'image en pièce jointe si disponible
            if (imageData != null && imageData.length > 0) {
                helper.addAttachment("alert-image.jpg", new ByteArrayResource(imageData));
            }

            mailSender.send(message);
            log.info("Email d'alerte envoyé: {} - {}", alert.getType(), alert.getSeverity());

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email d'alerte", e);
        }
    }

    @Override
    @Async("notificationExecutor")
    public void sendNotificationEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("Email de notification envoyé à: {}", to);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de notification", e);
        }
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
        return mailFrom != null && !mailFrom.isEmpty();
    }
}