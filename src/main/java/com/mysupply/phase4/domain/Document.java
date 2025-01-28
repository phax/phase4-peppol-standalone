package com.mysupply.phase4.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private byte[] document;

    protected Document() {

    }

    public Document(byte[] document) {
        this.document = document;
    }

    public byte[] getDocument() {
        return document;
    }

    public void setDocument(byte[] document) {
        this.document = document;
    }

    public UUID getId() {
        return id;
    }
}
