package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.config.StorageConfig;
import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.service.StorageService;
import com.banksecurity.backend.util.AsyncUtils;
import com.banksecurity.backend.util.Constants;
import com.banksecurity.backend.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final StorageConfig storageConfig;

    @jakarta.annotation.Resource(name = "storageExecutor")
    private Executor storageExecutor;

    @Override
    public String saveImage(MultipartFile file) throws IOException {
        try {
            // ✅ Utilisation de Constants.MAX_IMAGE_SIZE
            if (file.getSize() > Constants.MAX_IMAGE_SIZE) {
                throw new BadRequestException("Image trop volumineuse. Taille maximale: " +
                        (Constants.MAX_IMAGE_SIZE / (1024 * 1024)) + " MB");
            }
            return storageConfig.saveImage(file);
        } catch (IOException e) {
            log.error("Erreur lors de la sauvegarde de l'image: {}", e.getMessage(), e);
            throw new BadRequestException("Erreur lors de la sauvegarde de l'image", e);
        }
    }

    @Override
    public String saveVideo(MultipartFile file) throws IOException {
        try {
            // ✅ Utilisation de Constants.MAX_VIDEO_SIZE
            if (file.getSize() > Constants.MAX_VIDEO_SIZE) {
                throw new BadRequestException("Vidéo trop volumineuse. Taille maximale: " +
                        (Constants.MAX_VIDEO_SIZE / (1024 * 1024)) + " MB");
            }
            return storageConfig.saveVideo(file);
        } catch (IOException e) {
            log.error("Erreur lors de la sauvegarde de la vidéo: {}", e.getMessage(), e);
            throw new BadRequestException("Erreur lors de la sauvegarde de la vidéo", e);
        }
    }

    @Override
    public String saveImage(byte[] imageData, String filename) throws IOException {
        if (imageData == null || imageData.length == 0) {
            throw new BadRequestException("Données image vides");
        }

        // ✅ Utilisation de Constants.MAX_IMAGE_SIZE
        if (imageData.length > Constants.MAX_IMAGE_SIZE) {
            throw new BadRequestException("Image trop volumineuse. Taille maximale: " +
                    (Constants.MAX_IMAGE_SIZE / (1024 * 1024)) + " MB");
        }

        String newFilename = generateUniqueFilename(filename);
        Path targetPath = Paths.get(storageConfig.getImagesPath()).resolve(newFilename);

        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> {
                    try {
                        Files.write(targetPath, imageData);
                        return null;
                    } catch (IOException e) {
                        throw new RuntimeException("Erreur d'écriture du fichier", e);
                    }
                },
                storageExecutor,
                "Sauvegarde image " + newFilename
        );

        future.join();

        log.info("Image sauvegardée (bytes): {}", targetPath);
        return targetPath.toString();
    }

    @Override
    public org.springframework.core.io.Resource loadImage(String filename) {
        try {
            Path filePath = Paths.get(storageConfig.getImagesPath()).resolve(filename).normalize();
            org.springframework.core.io.Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Image", "filename", filename);
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (MalformedURLException e) {
            throw new BadRequestException("Chemin d'image invalide: " + filename, e);
        } catch (Exception e) {
            log.error("Erreur lors du chargement de l'image: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors du chargement de l'image: " + filename, e);
        }
    }

    @Override
    public org.springframework.core.io.Resource loadVideo(String filename) {
        try {
            Path filePath = Paths.get(storageConfig.getVideosPath()).resolve(filename).normalize();
            org.springframework.core.io.Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Vidéo", "filename", filename);
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (MalformedURLException e) {
            throw new BadRequestException("Chemin de vidéo invalide: " + filename, e);
        } catch (Exception e) {
            log.error("Erreur lors du chargement de la vidéo: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors du chargement de la vidéo: " + filename, e);
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
        cleanupDirectory(storageConfig.getImagesPath(), Constants.IMAGE_RETENTION_DAYS);
        cleanupDirectory(storageConfig.getVideosPath(), Constants.VIDEO_RETENTION_DAYS);
        log.info("Nettoyage des fichiers expirés effectué");
    }

    private void cleanupDirectory(String directoryPath, int daysToKeep) {
        try {
            Path directory = Paths.get(directoryPath);
            if (!Files.exists(directory)) {
                return;
            }

            LocalDateTime cutoff = DateUtils.daysAgo(daysToKeep);

            try (Stream<Path> files = Files.list(directory)) {
                files.filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                LocalDateTime lastModified = Files.getLastModifiedTime(file)
                                        .toInstant()
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toLocalDateTime();

                                if (DateUtils.isPast(lastModified) && lastModified.isBefore(cutoff)) {
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