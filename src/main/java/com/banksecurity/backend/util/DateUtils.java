package com.banksecurity.backend.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Utilitaire pour la manipulation des dates
 */
public final class DateUtils {

    private DateUtils() {
        throw new IllegalStateException("Classe utilitaire - ne pas instancier");
    }

    private static final DateTimeFormatter DEFAULT_FORMATTER =
            DateTimeFormatter.ofPattern(Constants.DATE_FORMAT);

    private static final DateTimeFormatter SHORT_FORMATTER =
            DateTimeFormatter.ofPattern(Constants.DATE_FORMAT_SHORT);

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern(Constants.TIME_FORMAT);

    /**
     * Convertit une Date en LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * Convertit une LocalDateTime en Date
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Formate une date en chaîne de caractères
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DEFAULT_FORMATTER);
    }

    /**
     * Formate date
     */
    public static String formatShort(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(SHORT_FORMATTER);
    }

    /**
     * Formate uniquement l'heure
     */
    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(TIME_FORMATTER);
    }

    /**
     * Parse une chaîne en LocalDateTime
     */
    public static LocalDateTime parse(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        return LocalDateTime.parse(dateStr, DEFAULT_FORMATTER);
    }

    /**
     * Calcule la différence en secondes entre deux dates
     */
    public static long secondsBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * Calcule la différence en minutes entre deux dates
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * Calcule la différence en heures entre deux dates
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * Calcule la différence en jours entre deux dates
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Ajoute des jours à une date
     */
    public static LocalDateTime addDays(LocalDateTime date, long days) {
        if (date == null) return null;
        return date.plusDays(days);
    }

    /**
     * Ajoute des heures à une date
     */
    public static LocalDateTime addHours(LocalDateTime date, long hours) {
        if (date == null) return null;
        return date.plusHours(hours);
    }

    /**
     * Ajoute des minutes à une date
     */
    public static LocalDateTime addMinutes(LocalDateTime date, long minutes) {
        if (date == null) return null;
        return date.plusMinutes(minutes);
    }

    /**
     * Retourne la date du début de journée
     */
    public static LocalDateTime startOfDay(LocalDateTime date) {
        if (date == null) return null;
        return date.toLocalDate().atStartOfDay();
    }

    /**
     * Retourne la date de fin de journée
     */
    public static LocalDateTime endOfDay(LocalDateTime date) {
        if (date == null) return null;
        return date.toLocalDate().atTime(23, 59, 59);
    }

    /**
     * Retourne la date d'il y a X jours
     */
    public static LocalDateTime daysAgo(long days) {
        return LocalDateTime.now().minusDays(days);
    }

    /**
     * Retourne la date d'il y a X heures
     */
    public static LocalDateTime hoursAgo(long hours) {
        return LocalDateTime.now().minusHours(hours);
    }

    /**
     * Vérifie si une date est dans le passé
     */
    public static boolean isPast(LocalDateTime date) {
        return date != null && date.isBefore(LocalDateTime.now());
    }

    /**
     * Vérifie si une date est dans le futur
     */
    public static boolean isFuture(LocalDateTime date) {
        return date != null && date.isAfter(LocalDateTime.now());
    }

    /**
     * Vérifie si une date est aujourd'hui
     */
    public static boolean isToday(LocalDateTime date) {
        if (date == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return date.toLocalDate().equals(now.toLocalDate());
    }

    /**
     * Vérifie si une date est cette semaine
     */
    public static boolean isThisWeek(LocalDateTime date) {
        if (date == null) return false;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1)
                .toLocalDate().atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusDays(7);
        return date.isAfter(startOfWeek) && date.isBefore(endOfWeek);
    }

    /**
     * Convertit en ZonedDateTime avec fuseau horaire
     */
    public static ZonedDateTime toZonedDateTime(LocalDateTime dateTime, String timezone) {
        if (dateTime == null) return null;
        return dateTime.atZone(ZoneId.of(timezone));
    }

    /**
     * Retourne le nom lisible d'une durée
     */
    public static String humanReadableDuration(LocalDateTime start, LocalDateTime end) {
        long seconds = secondsBetween(start, end);

        if (seconds < 60) {
            return seconds + " secondes";
        } else if (seconds < 3600) {
            return (seconds / 60) + " minutes";
        } else if (seconds < 86400) {
            return (seconds / 3600) + " heures";
        } else {
            return (seconds / 86400) + " jours";
        }
    }
}