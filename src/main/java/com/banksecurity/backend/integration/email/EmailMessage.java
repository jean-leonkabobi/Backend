package com.banksecurity.backend.integration.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

/**
 * Message email à envoyer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessage {

    /**
     * Expéditeur (optionnel, utilise la configuration par défaut si null)
     */
    private String from;

    /**
     * Destinataires
     */
    private String[] to;

    /**
     * Copie carbone (optionnel)
     */
    private String[] cc;

    /**
     * Sujet de l'email
     */
    private String subject;

    /**
     * Contenu de l'email (peut être HTML)
     */
    private String content;

    /**
     * Indique si le contenu est HTML
     */
    private boolean html;

    /**
     * Pièces jointes (optionnel)
     */
    private Attachment[] attachments;

    /**
     * Pièce jointe
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        private String filename;
        private File file;
    }
}