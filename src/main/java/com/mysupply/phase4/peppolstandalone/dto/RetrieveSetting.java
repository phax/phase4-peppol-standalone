package com.mysupply.phase4.peppolstandalone.dto;

import java.util.UUID;

public class RetrieveSetting {
    private UUID documentId;

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }
}
