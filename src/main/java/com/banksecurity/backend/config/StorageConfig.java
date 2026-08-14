package com.banksecurity.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
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
public class StorageConfig {

    @Value("${storage.images-path:./storage/images}")
    private String imagesPath;

    @Value("${storage.videos-path:./storage/videos}")
    private String videosPath;

    @Value("${storage.max-image-size:10485760}") // 10 MB par défaut
    private long maxImageSize;

    @Value("${storage.max-video-size:104857600}") // 100 MB par défaut
    private long maxVideoSize;

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
        try {
            Path imagesDir = Paths.get(imagesPath);
            Path videosDir = Paths.get(videosPath);

            if (!Files.exists(imagesDir)) {
                Files.createDirectories(imagesDir);
                log.info("Dossier d'images créé: {}", imagesDir.toAbsolutePath());
            }

            if (!Files.exists(videosDir)) {
                Files.createDirectories(videosDir);
                log.info("Dossier de vidéos créé: {}", videosDir.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Erreur lors de la création des dossiers de stockage", e);
            throw new StorageException("Impossible de créer les dossiers de stockage", e);
        }

        return properties;
    }

    /**
     * Sauvegarde une image
     */
    public String saveImage(MultipartFile file) throws IOException {
        validateImage(file);
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
        validateVideo(file);
        String filename = generateUniqueFilename(file.getOriginalFilename());
        Path targetPath = Paths.get(videosPath).resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Vidéo sauvegardée: {}", targetPath);
        return targetPath.toString();
    }

    /**
     * Charge une image comme ressource
     */
    public Resource loadImage(String filename) throws MalformedURLException {
        Path filePath = Paths.get(imagesPath).resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new StorageException("Image non trouvée: " + filename);
        }
    }

    /**
     * Charge une vidéo comme ressource
     */
    public Resource loadVideo(String filename) throws MalformedURLException {
        Path filePath = Paths.get(videosPath).resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new StorageException("Vidéo non trouvée: " + filename);
        }
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

    /**
     * Valide une image
     */
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Fichier image vide");
        }

        if (file.getSize() > maxImageSize) {
            throw new StorageException("Image trop volumineuse. Taille maximale: " +
                    (maxImageSize / (1024 * 1024)) + " MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new StorageException("Type de fichier non supporté pour une image: " + contentType);
        }
    }

    /**
     * Valide une vidéo
     */
    private void validateVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Fichier vidéo vide");
        }

        if (file.getSize() > maxVideoSize) {
            throw new StorageException("Vidéo trop volumineuse. Taille maximale: " +
                    (maxVideoSize / (1024 * 1024)) + " MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new StorageException("Type de fichier non supporté pour une vidéo: " + contentType);
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
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Classe interne pour les propriétés de stockage
     */
    public static class StorageProperties {
        private String imagesPath;
        private String videosPath;
        private long maxImageSize;
        private long maxVideoSize;

        // Getters et Setters
        public String getImagesPath() {
            return imagesPath;
        }

        public void setImagesPath(String imagesPath) {
            this.imagesPath = imagesPath;
        }

        public String getVideosPath() {
            return videosPath;
        }

        public void setVideosPath(String videosPath) {
            this.videosPath = videosPath;
        }

        public long getMaxImageSize() {
            return maxImageSize;
        }

        public void setMaxImageSize(long maxImageSize) {
            this.maxImageSize = maxImageSize;
        }

        public long getMaxVideoSize() {
            return maxVideoSize;
        }

        public void setMaxVideoSize(long maxVideoSize) {
            this.maxVideoSize = maxVideoSize;
        }
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