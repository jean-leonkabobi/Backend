package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.config.StorageConfig;
import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.service.StorageService;
import com.banksecurity.backend.util.AsyncUtils;
import com.banksecurity.backend.util.Constants;
import com.banksecurity.backend.util.DateUtils;
import com.banksecurity.backend.util.FileUtils;
import com.banksecurity.backend.util.ImageUtils;
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
import java.util.Date;
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
            if (file.getSize() > Constants.MAX_IMAGE_SIZE) {
                throw new BadRequestException("Image trop volumineuse. Taille maximale: " +
                        (Constants.MAX_IMAGE_SIZE / (1024 * 1024)) + " MB");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && !FileUtils.isImageFile(originalFilename)) {
                throw new BadRequestException("Type de fichier non supporté pour une image: " + originalFilename);
            }

            // ✅ Utilisation de ImageUtils.isValidImage
            byte[] imageData = file.getBytes();
            if (!ImageUtils.isValidImage(imageData)) {
                throw new BadRequestException("Le fichier n'est pas une image valide");
            }

            // ✅ Utilisation de ImageUtils.getImageDimensions
            java.awt.Dimension dimensions = ImageUtils.getImageDimensions(imageData);
            log.debug("Image originale: {}x{} pixels", dimensions.width, dimensions.height);

            return FileUtils.saveFile(file, storageConfig.getImagesPath());
        } catch (IOException e) {
            log.error("Erreur lors de la sauvegarde de l'image: {}", e.getMessage(), e);
            throw new BadRequestException("Erreur lors de la sauvegarde de l'image", e);
        }
    }

    @Override
    public String saveVideo(MultipartFile file) throws IOException {
        try {
            if (file.getSize() > Constants.MAX_VIDEO_SIZE) {
                throw new BadRequestException("Vidéo trop volumineuse. Taille maximale: " +
                        (Constants.MAX_VIDEO_SIZE / (1024 * 1024)) + " MB");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && !FileUtils.isVideoFile(originalFilename)) {
                throw new BadRequestException("Type de fichier non supporté pour une vidéo: " + originalFilename);
            }

            return FileUtils.saveFile(file, storageConfig.getVideosPath());
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

        if (imageData.length > Constants.MAX_IMAGE_SIZE) {
            throw new BadRequestException("Image trop volumineuse. Taille maximale: " +
                    (Constants.MAX_IMAGE_SIZE / (1024 * 1024)) + " MB");
        }

        // ✅ Utilisation de ImageUtils.isValidImage
        if (!ImageUtils.isValidImage(imageData)) {
            throw new BadRequestException("Les données ne sont pas une image valide");
        }

        String extension = FileUtils.getFileExtension(filename);
        if (extension.isEmpty()) {
            extension = ".jpg";
        }

        // ✅ Utilisation de ImageUtils.resizeImage (max 1920x1080)
        byte[] resizedImage = ImageUtils.resizeImage(imageData, 1920, 1080);
        log.debug("Image redimensionnée de {} à {} bytes", imageData.length, resizedImage.length);

        // ✅ Utilisation de ImageUtils.addWatermark
        byte[] watermarkedImage = ImageUtils.addWatermark(resizedImage, Constants.APP_NAME);
        log.debug("Filigrane ajouté");

        String savedPath = FileUtils.saveBytes(watermarkedImage, storageConfig.getImagesPath(), extension);
        log.info("Image sauvegardée (bytes) à {}: {}", DateUtils.formatTime(LocalDateTime.now()), savedPath);
        return savedPath;
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
        String filePath = Paths.get(storageConfig.getImagesPath()).resolve(filename).normalize().toString();
        boolean deleted = FileUtils.deleteFile(filePath);
        if (deleted) {
            log.info("Image supprimée avec succès: {}", filename);
        } else {
            log.warn("Image non trouvée pour suppression: {}", filename);
        }
    }

    @Override
    public void deleteVideo(String filename) {
        String filePath = Paths.get(storageConfig.getVideosPath()).resolve(filename).normalize().toString();
        boolean deleted = FileUtils.deleteFile(filePath);
        if (deleted) {
            log.info("Vidéo supprimée avec succès: {}", filename);
        } else {
            log.warn("Vidéo non trouvée pour suppression: {}", filename);
        }
    }

    @Override
    public boolean imageExists(String filename) {
        String filePath = Paths.get(storageConfig.getImagesPath()).resolve(filename).normalize().toString();
        return FileUtils.fileExists(filePath);
    }

    @Override
    public boolean videoExists(String filename) {
        String filePath = Paths.get(storageConfig.getVideosPath()).resolve(filename).normalize().toString();
        return FileUtils.fileExists(filePath);
    }

    @Override
    public void cleanupExpiredFiles() {
        cleanupDirectory(storageConfig.getImagesPath(), Constants.IMAGE_RETENTION_DAYS);
        cleanupDirectory(storageConfig.getVideosPath(), Constants.VIDEO_RETENTION_DAYS);
        log.info("Nettoyage des fichiers expirés effectué le {}", DateUtils.format(LocalDateTime.now()));
    }

    private void cleanupDirectory(String directoryPath, int daysToKeep) {
        try {
            Path directory = Paths.get(directoryPath);
            if (!Files.exists(directory)) {
                return;
            }

            LocalDateTime cutoff = DateUtils.addDays(LocalDateTime.now(), -daysToKeep);

            try (Stream<Path> files = Files.list(directory)) {
                files.filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                Date lastModifiedDate = new Date(Files.getLastModifiedTime(file).toMillis());
                                LocalDateTime lastModified = DateUtils.toLocalDateTime(lastModifiedDate);

                                if (DateUtils.isPast(lastModified) && lastModified.isBefore(cutoff)) {
                                    long fileSize = FileUtils.getFileSize(file.toString());
                                    log.debug("Taille du fichier à supprimer: {}", FileUtils.humanReadableSize(fileSize));
                                    boolean deleted = FileUtils.deleteFile(file.toString());
                                    if (deleted) {
                                        log.info("Fichier expiré supprimé: {}", file);
                                    }
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

        LocalDateTime now = DateUtils.parse(DateUtils.format(LocalDateTime.now()));
        String dateStr = DateUtils.formatShort(now).replace("-", "");
        String timeStr = DateUtils.formatTime(now).replace(":", "");
        String timestamp = dateStr + "_" + timeStr;

        return timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
    }
}