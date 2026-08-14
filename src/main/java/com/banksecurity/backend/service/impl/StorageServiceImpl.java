package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.config.StorageConfig;
import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final StorageConfig storageConfig;

    @Override
    public String saveImage(MultipartFile file) throws IOException {
        return storageConfig.saveImage(file);
    }

    @Override
    public String saveVideo(MultipartFile file) throws IOException {
        return storageConfig.saveVideo(file);
    }

    @Override
    public String saveImage(byte[] imageData, String filename) throws IOException {
        if (imageData == null || imageData.length == 0) {
            throw new BadRequestException("Données image vides");
        }

        String newFilename = generateUniqueFilename(filename);
        Path targetPath = Paths.get(storageConfig.getImagesPath()).resolve(newFilename);

        Files.write(targetPath, imageData);

        log.info("Image sauvegardée (bytes): {}", targetPath);
        return targetPath.toString();
    }

    @Override
    public Resource loadImage(String filename) {
        try {
            Path filePath = Paths.get(storageConfig.getImagesPath()).resolve(filename).normalize();
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
            Path filePath = Paths.get(storageConfig.getVideosPath()).resolve(filename).normalize();
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
        storageConfig.deleteImage(filename);
    }

    @Override
    public void deleteVideo(String filename) {
        storageConfig.deleteVideo(filename);
    }

    @Override
    public boolean imageExists(String filename) {
        Path filePath = Paths.get(storageConfig.getImagesPath()).resolve(filename).normalize();
        return Files.exists(filePath);
    }

    @Override
    public boolean videoExists(String filename) {
        Path filePath = Paths.get(storageConfig.getVideosPath()).resolve(filename).normalize();
        return Files.exists(filePath);
    }

    @Override
    public void cleanupExpiredFiles() {
        // Nettoyer les images de plus de 90 jours
        cleanupDirectory(storageConfig.getImagesPath(), 90);

        // Nettoyer les vidéos de plus de 30 jours
        cleanupDirectory(storageConfig.getVideosPath(), 30);

        log.info("Nettoyage des fichiers expirés effectué");
    }

    private void cleanupDirectory(String directoryPath, int daysToKeep) {
        try {
            Path directory = Paths.get(directoryPath);
            if (!Files.exists(directory)) {
                return;
            }

            LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);

            // Utilisation de try-with-resources pour fermer le Stream automatiquement
            try (Stream<Path> files = Files.list(directory)) {
                files.filter(Files::isRegularFile)
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
            }
        } catch (IOException e) {
            log.error("Erreur lors du nettoyage du répertoire: {}", directoryPath, e);
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