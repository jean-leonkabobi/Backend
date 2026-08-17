package com.banksecurity.backend.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Utilitaire pour la manipulation des images
 */
@Slf4j
public final class ImageUtils {

    private ImageUtils() {
        throw new IllegalStateException("Classe utilitaire - ne pas instancier");
    }

    /**
     * Encode une image en base64
     */
    public static String encodeToBase64(byte[] imageData) {
        return Base64.getEncoder().encodeToString(imageData);
    }

    /**
     * Décode une image depuis base64
     */
    public static byte[] decodeFromBase64(String base64Image) {
        return Base64.getDecoder().decode(base64Image);
    }

    /**
     * Redimensionne une image
     */
    public static byte[] resizeImage(byte[] imageData, int maxWidth, int maxHeight) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        BufferedImage originalImage = ImageIO.read(bais);

        if (originalImage == null) {
            throw new IOException("Format d'image non supporté");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double ratio = Math.min(
                (double) maxWidth / originalWidth,
                (double) maxHeight / originalHeight
        );

        if (ratio >= 1.0) {
            return imageData;
        }

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        Image resizedImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage bufferedResized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedResized.createGraphics();
        g2d.drawImage(resizedImage, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedResized, "jpg", baos);

        return baos.toByteArray();
    }

    /**
     * Ajoute un filigrane à une image
     */
    public static byte[] addWatermark(byte[] imageData, String watermarkText) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        BufferedImage originalImage = ImageIO.read(bais);

        if (originalImage == null) {
            throw new IOException("Format d'image non supporté");
        }

        Graphics2D g2d = (Graphics2D) originalImage.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(new Color(255, 255, 255, 128));

        int x = originalImage.getWidth() - 200;
        int y = originalImage.getHeight() - 20;
        g2d.drawString(watermarkText, x, y);

        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(originalImage, "jpg", baos);

        return baos.toByteArray();
    }

    /**
     * Vérifie si les données sont une image valide
     */
    public static boolean isValidImage(byte[] imageData) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(bais);
            return image != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Retourne les dimensions d'une image
     */
    public static Dimension getImageDimensions(byte[] imageData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        BufferedImage image = ImageIO.read(bais);

        if (image == null) {
            throw new IOException("Format d'image non supporté");
        }

        return new Dimension(image.getWidth(), image.getHeight());
    }
}