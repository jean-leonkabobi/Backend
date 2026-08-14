package com.banksecurity.backend.util;

/**
 * Constantes globales de l'application
 */
public final class Constants {

    // Constructeur privé pour empêcher l'instanciation
    private Constants() {
        throw new IllegalStateException("Classe utilitaire - ne pas instancier");
    }

    // ==================== SÉCURITÉ ====================
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    public static final String JWT_TYPE = "Bearer";
    public static final long JWT_EXPIRATION = 3600000L; // 1 heure
    public static final long JWT_REFRESH_EXPIRATION = 86400000L; // 24 heures

    // ==================== RÔLES ====================
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SECURITY = "ROLE_SECURITY";
    public static final String ROLE_MANAGER = "ROLE_MANAGER";

    // ==================== PAGINATION ====================
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_FIELD = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

    // ==================== ALERTES ====================
    public static final int DEFAULT_ALERT_RETENTION_DAYS = 30;
    public static final int MAX_ALERTS_PER_PAGE = 50;
    public static final long ALERT_PROCESSING_TIMEOUT = 300000L; // 5 minutes

    // ==================== CAMÉRAS ====================
    public static final long CAMERA_HEARTBEAT_TIMEOUT = 30000L; // 30 secondes
    public static final int DEFAULT_CAMERA_FPS = 15;
    public static final String DEFAULT_CAMERA_RESOLUTION = "1920x1080";
    public static final int MAX_CAMERAS_PER_SERVER = 10;

    // ==================== DÉTECTION IA ====================
    public static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.5;
    public static final double HIGH_CONFIDENCE_THRESHOLD = 0.75;
    public static final int DEFAULT_DETECTION_INTERVAL = 100; // millisecondes
    public static final int MAX_DETECTION_BATCH_SIZE = 10;

    // ==================== STOCKAGE ====================
    public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10 MB
    public static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100 MB
    public static final int VIDEO_RETENTION_DAYS = 30;
    public static final int IMAGE_RETENTION_DAYS = 90;

    // ==================== ZONES ====================
    public static final int DEFAULT_ZONE_SENSITIVITY = 50;
    public static final int MAX_ZONE_POINTS = 20;
    public static final int MIN_ZONE_POINTS = 3;

    // ==================== RÈGLES ====================
    public static final int DEFAULT_RULE_PRIORITY = 1;
    public static final int MAX_RULE_PRIORITY = 10;
    public static final int DEFAULT_PRESENCE_THRESHOLD = 180; // 3 minutes en secondes

    // ==================== EMAIL ====================
    public static final String EMAIL_FROM = "security@banksecurity.com";
    public static final String EMAIL_ALERT_SUBJECT = "🚨 Alerte de sécurité - ";
    public static final int MAX_EMAIL_RETRIES = 3;

    // ==================== AUDIT ====================
    public static final String AUDIT_ACTION_CREATE = "CREATE";
    public static final String AUDIT_ACTION_UPDATE = "UPDATE";
    public static final String AUDIT_ACTION_DELETE = "DELETE";
    public static final String AUDIT_ACTION_VIEW = "VIEW";
    public static final String AUDIT_ACTION_LOGIN = "LOGIN";
    public static final String AUDIT_ACTION_LOGOUT = "LOGOUT";
    public static final String AUDIT_ACTION_ALERT_CREATED = "ALERT_CREATED";
    public static final String AUDIT_ACTION_ALERT_RESOLVED = "ALERT_RESOLVED";

    // ==================== WEBSOCKET ====================
    public static final String WS_ENDPOINT = "/ws";
    public static final String WS_TOPIC_ALERTS = "/topic/alerts";
    public static final String WS_TOPIC_CAMERAS = "/topic/cameras";
    public static final String WS_TOPIC_STATS = "/topic/stats";
    public static final String WS_QUEUE_USER = "/queue/user";

    // ==================== DIVERS ====================
    public static final String APP_NAME = "Bank Security System";
    public static final String APP_VERSION = "1.0.0";
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT_SHORT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String TIMEZONE = "UTC";
}