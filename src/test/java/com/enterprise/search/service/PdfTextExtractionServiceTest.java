package com.enterprise.search.service;

import com.enterprise.search.dto.PageText;
import com.enterprise.search.exception.EmptyPdfException;
import com.enterprise.search.exception.InvalidPdfException;
import com.enterprise.search.exception.PdfFileNotFoundException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfTextExtractionServiceTest {

    private final PdfTextExtractionService extractionService = new PdfTextExtractionService();

    @TempDir
    Path temporaryDirectory;

    @Test
    void extractsTextForEachPage() throws IOException {
        Path pdf = temporaryDirectory.resolve("two-pages.pdf");
        createPdf(pdf);

        List<PageText> pages = extractionService.extractPageText(pdf);

        assertEquals(2, pages.size());
        assertEquals(1, pages.getFirst().pageNumber());
        assertEquals("First page", pages.getFirst().text().trim());
        assertEquals(2, pages.get(1).pageNumber());
        assertEquals("Second page", pages.get(1).text().trim());
    }

    @Test
    void returnsTheActualPageCountForSingleAndMultiplePagePdfs() throws IOException {
        Path singlePagePdf = temporaryDirectory.resolve("single-page.pdf");
        try (PDDocument document = new PDDocument()) {
            addPage(document, "Only page");
            document.save(singlePagePdf.toFile());
        }
        Path multiplePagePdf = temporaryDirectory.resolve("multiple-pages.pdf");
        createPdf(multiplePagePdf);

        assertEquals(1, extractionService.getPageCount(singlePagePdf));
        assertEquals(2, extractionService.getPageCount(multiplePagePdf));
    }

    @Test
    void rejectsMissingPdf() {
        assertThrows(PdfFileNotFoundException.class,
                () -> extractionService.extractPageText(temporaryDirectory.resolve("missing.pdf")));
    }

    @Test
    void rejectsInvalidPdf() throws IOException {
        Path invalidPdf = temporaryDirectory.resolve("invalid.pdf");
        Files.writeString(invalidPdf, "not a PDF");

        assertThrows(InvalidPdfException.class,
                () -> extractionService.extractPageText(invalidPdf));
    }

    @Test
    void rejectsPdfWithoutPages() throws IOException {
        Path emptyPdf = temporaryDirectory.resolve("empty.pdf");
        try (PDDocument document = new PDDocument()) {
            document.save(emptyPdf.toFile());
        }

        assertThrows(EmptyPdfException.class,
                () -> extractionService.extractPageText(emptyPdf));
    }

    private void createPdf(Path path) throws IOException {
        try (PDDocument document = new PDDocument()) {
            addPage(document, "First page");
            addPage(document, "Second page");
            document.save(path.toFile());
        }
    }

    private void addPage(PDDocument document, String text) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream.newLineAtOffset(72, 720);
            contentStream.showText(text);
            contentStream.endText();
        }
    }
}
