package com.banksecurity.backend.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    /**
     * Sauvegarde une image
     */
    String saveImage(MultipartFile file) throws IOException;

    /**
     * Sauvegarde une vidéo
     */
    String saveVideo(MultipartFile file) throws IOException;

    /**
     * Sauvegarde une image à partir de bytes
     */
    String saveImage(byte[] imageData, String filename) throws IOException;

    /**
     * Charge une image
     */
    Resource loadImage(String filename);

    /**
     * Charge une vidéo
     */
    Resource loadVideo(String filename);

    /**
     * Supprime une image
     */
    void deleteImage(String filename);

    /**
     * Supprime une vidéo
     */
    void deleteVideo(String filename);

    /**
     * Vérifie si une image existe
     */
    boolean imageExists(String filename);

    /**
     * Vérifie si une vidéo existe
     */
    boolean videoExists(String filename);

    /**
     * Nettoie les fichiers expirés
     */
    void cleanupExpiredFiles();
}