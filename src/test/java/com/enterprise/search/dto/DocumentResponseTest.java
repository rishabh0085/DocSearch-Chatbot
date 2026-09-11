package com.enterprise.search.dto;

import com.enterprise.search.constants.DocumentStatus;
import com.enterprise.search.entity.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentResponseTest {

    @Test
    void includesThePersistedPageCount() {
        Document document = new Document();
        document.setId(3L);
        document.setFileName("policy.pdf");
        document.setFileType("application/pdf");
        document.setFileSize(126000L);
        document.setPageCount(5);
        document.setStatus(DocumentStatus.READY);

        assertEquals(5, DocumentResponse.from(document).getPageCount());
    }
}
