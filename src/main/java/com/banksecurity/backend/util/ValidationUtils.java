package com.banksecurity.backend.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Utilitaire de validation
 */
@Slf4j
public final class ValidationUtils {

    private ValidationUtils() {
        throw new IllegalStateException("Classe utilitaire - ne pas instancier");
    }

    // Patterns de validation
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[0-9]{10,15}$");

    private static final Pattern IP_ADDRESS_PATTERN =
            Pattern.compile("^([0-9]{1,3}\\.){3}[0-9]{1,3}$");

    private static final Pattern RTSP_URL_PATTERN =
            Pattern.compile("^rtsp://.*");

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");

    // ==================== MÉTHODES POSITIVES ====================

    /**
     * Valide une adresse email
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Valide un numéro de téléphone
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Valide une adresse IP
     */
    public static boolean isValidIpAddress(String ipAddress) {
        if (ipAddress == null || !IP_ADDRESS_PATTERN.matcher(ipAddress).matches()) {
            return false;
        }

        String[] parts = ipAddress.split("\\.");
        for (String part : parts) {
            int value = Integer.parseInt(part);
            if (value < 0 || value > 255) {
                return false;
            }
        }
        return true;
    }

    /**
     * Valide une URL RTSP
     */
    public static boolean isValidRtspUrl(String url) {
        return url != null && RTSP_URL_PATTERN.matcher(url).matches();
    }

    /**
     * Valide la force d'un mot de passe
     */
    public static boolean isStrongPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Valide une sensibilité (0-100)
     */
    public static boolean isValidSensitivity(int sensitivity) {
        return sensitivity >= 0 && sensitivity <= 100;
    }

    /**
     * Valide une priorité (1-10)
     */
    public static boolean isValidPriority(int priority) {
        return priority >= 1 && priority <= 10;
    }

    /**
     * Valide les points d'un polygone
     */
    public static boolean isValidPolygonPoints(String points) {
        if (points == null || points.isEmpty()) {
            return false;
        }

        try {
            String[] pointArray = points.replaceAll("[\\[\\]]", "").split(",");
            return pointArray.length >= 6;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Valide une résolution de caméra
     */
    public static boolean isValidResolution(String resolution) {
        if (resolution == null) return false;

        Pattern resolutionPattern = Pattern.compile("^\\d{3,4}x\\d{3,4}$");
        if (!resolutionPattern.matcher(resolution).matches()) {
            return false;
        }

        String[] parts = resolution.split("x");
        int width = Integer.parseInt(parts[0]);
        int height = Integer.parseInt(parts[1]);

        return width > 0 && height > 0 && width <= 7680 && height <= 4320;
    }

    /**
     * Valide un FPS de caméra
     */
    public static boolean isValidFps(int fps) {
        return fps > 0 && fps <= 60;
    }

    /**
     * Nettoie une chaîne de caractères (supprime les espaces superflus)
     */
    public static String sanitizeString(String input) {
        if (input == null) return null;
        return input.trim().replaceAll("\\s+", " ");
    }

    /**
     * Vérifie si une chaîne est vide ou null
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    // ==================== MÉTHODES NÉGATIVES (Évitent les inversions) ====================

    /**
     * ✅ Vérifie si une adresse email est invalide
     */
    public static boolean isInvalidEmail(String email) {
        return !isValidEmail(email);
    }

    /**
     * ✅ Vérifie si un numéro de téléphone est invalide
     */
    public static boolean isInvalidPhone(String phone) {
        return !isValidPhone(phone);
    }

    /**
     * ✅ Vérifie si une URL RTSP est invalide
     */
    public static boolean isInvalidRtspUrl(String url) {
        return !isValidRtspUrl(url);
    }

    /**
     * ✅ Vérifie si un mot de passe est faible
     */
    public static boolean isWeakPassword(String password) {
        return !isStrongPassword(password);
    }

    /**
     * ✅ Vérifie si une sensibilité est invalide
     */
    public static boolean isInvalidSensitivity(int sensitivity) {
        return !isValidSensitivity(sensitivity);
    }

    /**
     * ✅ Vérifie si une résolution est invalide
     */
    public static boolean isInvalidResolution(String resolution) {
        return !isValidResolution(resolution);
    }

    /**
     * ✅ Vérifie si un FPS est invalide
     */
    public static boolean isInvalidFps(int fps) {
        return !isValidFps(fps);
    }
}