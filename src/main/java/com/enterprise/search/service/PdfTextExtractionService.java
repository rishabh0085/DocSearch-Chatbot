package com.enterprise.search.service;

import com.enterprise.search.dto.PageText;
import com.enterprise.search.exception.EmptyPdfException;
import com.enterprise.search.exception.InvalidPdfException;
import com.enterprise.search.exception.PdfFileNotFoundException;
import com.enterprise.search.exception.PdfReadException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfTextExtractionService {

    private static final int PDF_HEADER_SEARCH_LIMIT = 1_024;

    /**
     * Extracts text independently from each page in the supplied PDF.
     *
     * @param pdfPath path to a locally stored uploaded PDF
     * @return one result per page, using one-based page numbers
     */
    public List<PageText> extractPageText(Path pdfPath) {
        validatePdfFile(pdfPath);

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                throw new EmptyPdfException();
            }

            PDFTextStripper textStripper = new PDFTextStripper();
            List<PageText> pages = new ArrayList<>(pageCount);
            for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                textStripper.setStartPage(pageNumber);
                textStripper.setEndPage(pageNumber);
                pages.add(new PageText(pageNumber, textStripper.getText(document)));
            }
            return List.copyOf(pages);
        } catch (EmptyPdfException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PdfReadException(exception);
        }
    }

    /** Returns the number of pages in a valid, non-empty PDF. */
    public int getPageCount(Path pdfPath) {
        validatePdfFile(pdfPath);

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                throw new EmptyPdfException();
            }
            return pageCount;
        } catch (EmptyPdfException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PdfReadException(exception);
        }
    }

    private void validatePdfFile(Path pdfPath) {
        if (pdfPath == null || !Files.isRegularFile(pdfPath)) {
            throw new PdfFileNotFoundException();
        }
        if (!Files.isReadable(pdfPath)) {
            throw new PdfReadException(new IOException("PDF file is not readable"));
        }
        if (!hasPdfHeader(pdfPath)) {
            throw new InvalidPdfException();
        }
    }

    private boolean hasPdfHeader(Path pdfPath) {
        byte[] header = new byte[PDF_HEADER_SEARCH_LIMIT];
        try (InputStream inputStream = Files.newInputStream(pdfPath)) {
            int bytesRead = inputStream.read(header);
            for (int index = 0; index <= bytesRead - 5; index++) {
                if (header[index] == '%' && header[index + 1] == 'P'
                        && header[index + 2] == 'D' && header[index + 3] == 'F'
                        && header[index + 4] == '-') {
                    return true;
                }
            }
            return false;
        } catch (IOException exception) {
            throw new PdfReadException(exception);
        }
    }
}
