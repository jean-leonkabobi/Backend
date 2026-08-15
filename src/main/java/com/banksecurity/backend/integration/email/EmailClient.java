package com.banksecurity.backend.integration.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Client email pour l'envoi de notifications
 */
@Slf4j
@Component
public class EmailClient {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String defaultFrom;

    public EmailClient(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envoie un email simple
     */
    public void sendSimpleEmail(EmailMessage message) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(message.getFrom() != null ? message.getFrom() : defaultFrom);
            mailMessage.setTo(message.getTo());
            mailMessage.setSubject(message.getSubject());
            mailMessage.setText(message.getContent());

            if (message.getCc() != null && message.getCc().length > 0) {
                mailMessage.setCc(message.getCc());
            }

            mailSender.send(mailMessage);
            log.info("Email simple envoyé à: {}", String.join(", ", message.getTo()));

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email simple", e);
            throw new RuntimeException("Erreur d'envoi d'email", e);
        }
    }

    /**
     * Envoie un email HTML avec pièces jointes
     */
    public void sendHtmlEmail(EmailMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(message.getFrom() != null ? message.getFrom() : defaultFrom);
            helper.setTo(message.getTo());
            helper.setSubject(message.getSubject());
            helper.setText(message.getContent(), true);

            if (message.getCc() != null && message.getCc().length > 0) {
                helper.setCc(message.getCc());
            }

            // Ajouter les pièces jointes
            if (message.getAttachments() != null) {
                for (EmailMessage.Attachment attachment : message.getAttachments()) {
                    helper.addAttachment(attachment.getFilename(), attachment.getFile());
                }
            }

            mailSender.send(mimeMessage);
            log.info("Email HTML envoyé à: {}", String.join(", ", message.getTo()));

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email HTML", e);
            throw new RuntimeException("Erreur d'envoi d'email HTML", e);
        }
    }

    /**
     * Vérifie si le client email est configuré
     */
    public boolean isConfigured() {
        return defaultFrom != null && !defaultFrom.isEmpty();
    }
}