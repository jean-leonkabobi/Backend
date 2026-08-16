package com.banksecurity.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Utilitaire pour la gestion des fichiers
 */
@Slf4j
public final class FileUtils {

    private FileUtils() {
        throw new IllegalStateException("Classe utilitaire - ne pas instancier");
    }

    /**
     * Sauvegarde un fichier uploadé
     * @return Le chemin du fichier sauvegardé
     */
    public static String saveFile(MultipartFile file, String directory) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        Path uploadPath = Paths.get(directory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID() + extension;

        Path targetPath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // ✅ Message de log différencié
        log.info("[FILE-UPLOAD] Fichier uploadé: {} (source: {})", targetPath, originalFilename);
        return targetPath.toString();
    }

    /**
     * Sauvegarde des bytes dans un fichier
     */
    public static String saveBytes(byte[] data, String directory, String extension) throws IOException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Les données sont vides");
        }

        Path uploadPath = Paths.get(directory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String newFilename = UUID.randomUUID() + extension;

        Path targetPath = uploadPath.resolve(newFilename);
        Files.write(targetPath, data);

        // ✅ Message de log différencié
        log.info("[FILE-BYTES] Fichier écrit depuis bytes: {} (taille: {} bytes)", targetPath, data.length);
        return targetPath.toString();
    }

    /**
     * Supprime un fichier
     * Le retour boolean indique si la suppression a réussi
     */
    public static boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            log.warn("[FILE-DELETE] Chemin de fichier vide");
            return false;
        }

        try {
            Path path = Paths.get(filePath);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                // ✅ Message de log différencié
                log.info("[FILE-DELETE] Fichier supprimé avec succès: {}", path);
            } else {
                log.debug("[FILE-DELETE] Fichier non trouvé (rien à supprimer): {}", path);
            }
            return deleted;
        } catch (IOException e) {
            log.error("[FILE-DELETE] Erreur lors de la suppression du fichier: {}", filePath, e);
            return false;
        }
    }

    /**
     * Vérifie si un fichier existe
     */
    public static boolean fileExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Retourne l'extension d'un fichier
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }

        return filename.substring(lastDotIndex).toLowerCase();
    }

    /**
     * Vérifie si le fichier est une image
     */
    public static boolean isImageFile(String filename) {
        String extension = getFileExtension(filename);
        return extension.equals(".jpg") ||
                extension.equals(".jpeg") ||
                extension.equals(".png") ||
                extension.equals(".gif") ||
                extension.equals(".bmp");
    }

    /**
     * Vérifie si le fichier est une vidéo
     */
    public static boolean isVideoFile(String filename) {
        String extension = getFileExtension(filename);
        return extension.equals(".mp4") ||
                extension.equals(".avi") ||
                extension.equals(".mov") ||
                extension.equals(".wmv") ||
                extension.equals(".flv");
    }

    /**
     * Retourne la taille d'un fichier en bytes
     */
    public static long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            log.error("[FILE-SIZE] Erreur lors de la lecture de la taille du fichier: {}", filePath, e);
            return 0;
        }
    }

    /**
     * Formate la taille d'un fichier en format lisible
     */
    public static String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}