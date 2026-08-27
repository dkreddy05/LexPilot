package com.lexpilot.ingestion.service;

import com.lexpilot.common.exception.ExtractionException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

/**
 * Extracts plain text and metadata from document byte streams using Apache Tika.
 * <p>
 * For this slice, only text-based PDFs are supported. Scanned PDFs (no embedded
 * text layer) will fail loudly rather than silently producing zero chunks.
 */
@Service
public class TikaExtractionService {

    private static final Logger log = LoggerFactory.getLogger(TikaExtractionService.class);

    /**
     * Minimum characters of extracted text to consider a PDF valid.
     * Below this threshold we assume it is a scanned/image-only PDF.
     */
    private static final int MIN_TEXT_LENGTH = 50;

    /** Max characters BodyContentHandler will buffer (10M chars ~10MB plain text limit to prevent DoS/OOM). */
    private static final int WRITE_LIMIT = 10 * 1024 * 1024;

    private final AutoDetectParser parser = new AutoDetectParser();

    /**
     * Extract text and metadata from a document byte stream.
     *
     * @param fileBytes raw file bytes
     * @param mimeType  MIME type (e.g. {@code application/pdf})
     * @return extraction result containing text and metadata
     * @throws ExtractionException if Tika fails or the document yields insufficient text
     */
    public ExtractionResult extract(byte[] fileBytes, String mimeType) {
        Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, mimeType);
        BodyContentHandler handler = new BodyContentHandler(WRITE_LIMIT);

        try (ByteArrayInputStream stream = new ByteArrayInputStream(fileBytes)) {
            parser.parse(stream, handler, metadata, new ParseContext());
        } catch (Exception e) {
            log.error("Tika extraction failed for mime-type={}", mimeType, e);
            throw new ExtractionException("Text extraction failed: " + e.getMessage(), e);
        }

        String text = handler.toString().trim();

        // Fail loudly on scanned / image-only PDFs rather than producing zero chunks
        if (text.length() < MIN_TEXT_LENGTH) {
            throw new ExtractionException(
                    "Extracted text is too short (" + text.length() + " chars). "
                    + "The document may be a scanned image without an embedded text layer. "
                    + "OCR is not supported in this version.");
        }

        int pageCount = parsePageCount(metadata);
        String title = metadata.get(TikaCoreProperties.TITLE);

        log.info("Tika extraction complete: {} chars, {} pages, title='{}'",
                 text.length(), pageCount, title);

        return new ExtractionResult(text, pageCount, title);
    }

    private int parsePageCount(Metadata metadata) {
        String pages = metadata.get("xmpTPg:NPages");
        if (pages == null) {
            pages = metadata.get("meta:page-count");
        }
        if (pages != null) {
            try {
                return Integer.parseInt(pages);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return -1;
    }
}
