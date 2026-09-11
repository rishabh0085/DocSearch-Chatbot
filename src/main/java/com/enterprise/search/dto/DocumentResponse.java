package com.enterprise.search.dto;

import com.enterprise.search.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DocumentResponse {

    private final Long id;
    private final String fileName;
    private final String fileType;
    private final Long fileSize;
    private final Integer pageCount;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getFileName(),
                document.getFileType(),
                document.getFileSize(),
                document.getPageCount(),
                document.getStatus() == null ? null : document.getStatus().name(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
