package com.mysupply.phase4.domain;

import com.mysupply.phase4.persistence.DocumentConstants;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import java.util.UUID;

@Entity
@Table(name = DocumentConstants.DOCUMENT_TABLE_NAME, schema = DocumentConstants.DOCUMENT_SCHEMA_NAME)
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
