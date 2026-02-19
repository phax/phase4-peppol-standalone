package com.mysupply.phase4.peppolstandalone.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RetrieveSearchResult {
    private List<UUID> documentIds;

    public RetrieveSearchResult() {
        documentIds = new ArrayList<UUID>();
    }

    public void addDocumentId(UUID documentId) {
        this.documentIds.add(documentId);
    }

    public List<UUID> getDocumentIds() {
        return this.documentIds;
    }

    public void setDocumentIds(List<UUID> documentIds) {
        this.documentIds = documentIds;
    }
}
