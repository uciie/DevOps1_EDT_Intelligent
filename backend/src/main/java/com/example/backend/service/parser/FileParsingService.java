package com.example.backend.service.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class FileParsingService {

    /**
     * Méthode principale qui détecte le type de fichier 
     * et extrait le texte brut.
     */
    public String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();

        if (contentType == null) {
            throw new IllegalArgumentException("Le type de fichier est inconnu.");
        }

        if (contentType.equals("application/pdf")) {
            return parsePdf(file);
        } else if (contentType.equals("text/plain")) {
            return parseText(file);
        } else {
            throw new IllegalArgumentException("Format non supporté : " + contentType);
        }
    }

    // Extraction pour les fichiers .txt
    private String parseText(MultipartFile file) throws IOException {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    // Extraction pour les fichiers .pdf via PDFBox
    private String parsePdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new IOException("Erreur lors de la lecture du PDF : " + e.getMessage());
        }
    }
}