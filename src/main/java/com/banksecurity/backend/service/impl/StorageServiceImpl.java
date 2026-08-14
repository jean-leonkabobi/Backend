package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

    @Value("${storage.images-path:./storage/images}")
    private String imagesPath;

    @Value("${storage.videos-path:./storage/videos}")
    private String videosPath;

    @Value("${storage.max-image-size:10485760}")
    private long maxImageSize;

    @Value("${storage.max-video-size:104857600}")
    private long maxVideoSize;

    @Override
    public String saveImage(MultipartFile file) throws IOException {
        validateImage(file);

        String filename = generateUniqueFilename(file.getOriginalFilename());
        Path targetPath = Paths.get(imagesPath).resolve(filename);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Image sauvegardée: {}", targetPath);
        return targetPath.toString();
    }

    @Override
    public String saveVideo(MultipartFile file) throws IOException {
        validateVideo(file);

        String filename = generateUniqueFilename(file.getOriginalFilename());
        Path targetPath = Paths.get(videosPath).resolve(filename);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Vidéo sauvegardée: {}", targetPath);
        return targetPath.toString();
    }

    @Override
    public String saveImage(byte[] imageData, String filename) throws IOException {
        if (imageData == null || imageData.length == 0) {
            throw new BadRequestException("Données image vides");
        }

        String newFilename = generateUniqueFilename(filename);
        Path targetPath = Paths.get(imagesPath).resolve(newFilename);

        Files.write(targetPath, imageData);

        log.info("Image sauvegardée (bytes): {}", targetPath);
        return targetPath.toString();
    }

    @Override
    public Resource loadImage(String filename) {
        try {
            Path filePath = Paths.get(imagesPath).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Image", "filename", filename);
            }
        } catch (MalformedURLException e) {
            throw new BadRequestException("Chemin d'image invalide: " + filename);
        }
    }

    @Override
    public Resource loadVideo(String filename) {
        try {
            Path filePath = Paths.get(videosPath).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Vidéo", "filename", filename);
            }
        } catch (MalformedURLException e) {
            throw new BadRequestException("Chemin de vidéo invalide: " + filename);
        }
    }

    @Override
    public void deleteImage(String filename) {
        try {
            Path filePath = Paths.get(imagesPath).resolve(filename).normalize();
            Files.deleteIfExists(filePath);
            log.info("Image supprimée: {}", filePath);
        } catch (IOException e) {
            log.error("Erreur lors de la suppression de l'image: {}", filename, e);
        }
    }

    @Override
    public void deleteVideo(String filename) {
        try {
            Path filePath = Paths.get(videosPath).resolve(filename).normalize();
            Files.deleteIfExists(filePath);
            log.info("Vidéo supprimée: {}", filePath);
        } catch (IOException e) {
            log.error("Erreur lors de la suppression de la vidéo: {}", filename, e);
        }
    }

    @Override
    public boolean imageExists(String filename) {
        Path filePath = Paths.get(imagesPath).resolve(filename).normalize();
        return Files.exists(filePath);
    }

    @Override
    public boolean videoExists(String filename) {
        Path filePath = Paths.get(videosPath).resolve(filename).normalize();
        return Files.exists(filePath);
    }

    @Override
    public void cleanupExpiredFiles() {
        // Nettoyer les images de plus de 90 jours
        cleanupDirectory(imagesPath, 90);

        // Nettoyer les vidéos de plus de 30 jours
        cleanupDirectory(videosPath, 30);

        log.info("Nettoyage des fichiers expirés effectué");
    }

    private void cleanupDirectory(String directoryPath, int daysToKeep) {
        try {
            Path directory = Paths.get(directoryPath);
            if (!Files.exists(directory)) {
                return;
            }

            LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);

            Files.list(directory)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            LocalDateTime lastModified = Files.getLastModifiedTime(file)
                                    .toInstant()
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDateTime();

                            if (lastModified.isBefore(cutoff)) {
                                Files.deleteIfExists(file);
                                log.info("Fichier expiré supprimé: {}", file);
                            }
                        } catch (IOException e) {
                            log.error("Erreur lors de la vérification du fichier: {}", file, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Erreur lors du nettoyage du répertoire: {}", directoryPath, e);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Fichier image vide");
        }

        if (file.getSize() > maxImageSize) {
            throw new BadRequestException("Image trop volumineuse. Taille maximale: " +
                    (maxImageSize / (1024 * 1024)) + " MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Type de fichier non supporté pour une image: " + contentType);
        }
    }

    private void validateVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Fichier vidéo vide");
        }

        if (file.getSize() > maxVideoSize) {
            throw new BadRequestException("Vidéo trop volumineuse. Taille maximale: " +
                    (maxVideoSize / (1024 * 1024)) + " MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new BadRequestException("Type de fichier non supporté pour une vidéo: " + contentType);
        }
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
    }
}