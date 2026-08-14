package com.banksecurity.backend.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Configuration du stockage des fichiers (images, vidéos)
 */
@Slf4j
@Configuration
@Getter
public class StorageConfig {

    @Value("${storage.images-path:./storage/images}")
    private String imagesPath;

    @Value("${storage.videos-path:./storage/videos}")
    private String videosPath;

    @Value("${storage.max-image-size:10485760}") // 10 MB par défaut
    private long maxImageSize;

    @Value("${storage.max-video-size:104857600}") // 100 MB par défaut
    private long maxVideoSize;

    // ==================== BEAN ====================

    /**
     * Initialise les dossiers de stockage au démarrage
     */
    @Bean
    public StorageProperties storageProperties() {
        StorageProperties properties = new StorageProperties();
        properties.setImagesPath(imagesPath);
        properties.setVideosPath(videosPath);
        properties.setMaxImageSize(maxImageSize);
        properties.setMaxVideoSize(maxVideoSize);

        // Créer les dossiers s'ils n'existent pas
        createStorageDirectories();

        return properties;
    }

    // ==================== MÉTHODES PUBLIQUES ====================

    /**
     * Sauvegarde une image
     */
    public String saveImage(MultipartFile file) throws IOException {
        validateFile(file, maxImageSize, "image", "image");
        String filename = generateUniqueFilename(file.getOriginalFilename());
        Path targetPath = Paths.get(imagesPath).resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Image sauvegardée: {}", targetPath);
        return targetPath.toString();
    }

    /**
     * Sauvegarde une vidéo
     */
    public String saveVideo(MultipartFile file) throws IOException {
        validateFile(file, maxVideoSize, "video", "vidéo");
        String filename = generateUniqueFilename(file.getOriginalFilename());
        Path targetPath = Paths.get(videosPath).resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Vidéo sauvegardée: {}", targetPath);
        return targetPath.toString();
    }

    /**
     * Supprime une image
     */
    public void deleteImage(String filename) {
        try {
            Path filePath = Paths.get(imagesPath).resolve(filename).normalize();
            Files.deleteIfExists(filePath);
            log.info("Image supprimée: {}", filePath);
        } catch (IOException e) {
            log.error("Erreur lors de la suppression de l'image: {}", filename, e);
        }
    }

    /**
     * Supprime une vidéo
     */
    public void deleteVideo(String filename) {
        try {
            Path filePath = Paths.get(videosPath).resolve(filename).normalize();
            Files.deleteIfExists(filePath);
            log.info("Vidéo supprimée: {}", filePath);
        } catch (IOException e) {
            log.error("Erreur lors de la suppression de la vidéo: {}", filename, e);
        }
    }

    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Crée les dossiers de stockage s'ils n'existent pas
     */
    private void createStorageDirectories() {
        createDirectory(imagesPath, "images");
        createDirectory(videosPath, "vidéos");
    }

    /**
     * Crée un dossier de stockage s'il n'existe pas
     */
    private void createDirectory(String path, String type) {
        try {
            Path directory = Paths.get(path);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                log.info("Dossier de {} créé: {}", type, directory.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Erreur lors de la création du dossier de {}", type, e);
            throw new StorageException("Impossible de créer le dossier de " + type, e);
        }
    }

    /**
     * Valide un fichier (image ou vidéo)
     */
    private void validateFile(MultipartFile file, long maxSize, String expectedType, String typeLabel) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Fichier " + typeLabel + " vide");
        }

        if (file.getSize() > maxSize) {
            throw new StorageException(typeLabel.substring(0, 1).toUpperCase() + typeLabel.substring(1) +
                    " trop volumineux. Taille maximale: " + (maxSize / (1024 * 1024)) + " MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith(expectedType + "/")) {
            throw new StorageException("Type de fichier non supporté pour une " + typeLabel + ": " + contentType);
        }
    }

    /**
     * Génère un nom de fichier unique
     */
    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }

    // ==================== CLASSES INTERNES ====================

    /**
     * Classe interne pour les propriétés de stockage
     */
    @Getter
    @Setter
    public static class StorageProperties {
        private String imagesPath;
        private String videosPath;
        private long maxImageSize;
        private long maxVideoSize;
    }

    /**
     * Exception personnalisée pour le stockage
     */
    public static class StorageException extends RuntimeException {
        public StorageException(String message) {
            super(message);
        }

        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}