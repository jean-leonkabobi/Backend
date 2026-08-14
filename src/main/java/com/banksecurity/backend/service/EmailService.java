package com.banksecurity.backend.service;

import com.banksecurity.backend.model.Alert;
import com.banksecurity.backend.model.User;

public interface EmailService {

    /**
     * Envoie un email d'alerte
     */
    void sendAlertEmail(Alert alert, byte[] imageData);

    /**
     * Envoie un email de notification
     */
    void sendNotificationEmail(String to, String subject, String content);

    /**
     * Envoie un email de bienvenue
     */
    void sendWelcomeEmail(User user);

    /**
     * Envoie un email de réinitialisation de mot de passe
     */
    void sendPasswordResetEmail(User user, String resetToken);

    /**
     * Envoie un email de compte verrouillé
     */
    void sendAccountLockedEmail(User user);

    /**
     * Envoie un email de compte déverrouillé
     */
    void sendAccountUnlockedEmail(User user);

    /**
     * Envoie un rapport quotidien
     */
    void sendDailyReportEmail(String to, String reportContent);

    /**
     * Vérifie si le service email est configuré
     */
    boolean isEmailConfigured();
}