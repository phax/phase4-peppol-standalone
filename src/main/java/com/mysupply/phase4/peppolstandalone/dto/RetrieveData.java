package com.mysupply.phase4.peppolstandalone.dto;

import com.mysupply.phase4.domain.Document;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RetrieveData {
    private UUID id;
    private byte[] data;
    private OffsetDateTime created;
    private String domain;
    private String senderIdentifier;
    private String receiverIdentifier;
    private String docType;
    private String process;
    private String senderCertificate;
    private String receiverCertificate;
    private String protocol;
    private String conversationId;
    private String messageId;

    public RetrieveData() {
    }

    public RetrieveData(Document document) {
        this.id = document.getId();
        this.data = document.getData();
        this.created = document.getCreated();
        this.domain = document.getDomain();
        this.senderIdentifier = document.getSenderIdentifier();
        this.receiverIdentifier = document.getReceiverIdentifier();
        this.docType = document.getDocType();
        this.process = document.getProcess();
        this.senderCertificate = document.getSenderCertificate();
        this.receiverCertificate = document.getReceiverCertificate();
        this.protocol = document.getProtocol();
        this.conversationId = document.getConversationId();
        this.messageId = document.getMessageId();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSenderIdentifier() {
        return senderIdentifier;
    }

    public void setSenderIdentifier(String senderIdentifier) {
        this.senderIdentifier = senderIdentifier;
    }

    public String getReceiverIdentifier() {
        return receiverIdentifier;
    }

    public void setReceiverIdentifier(String receiverIdentifier) {
        this.receiverIdentifier = receiverIdentifier;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public String getSenderCertificate() {
        return senderCertificate;
    }

    public void setSenderCertificate(String senderCertificate) {
        this.senderCertificate = senderCertificate;
    }

    public String getReceiverCertificate() {
        return receiverCertificate;
    }

    public void setReceiverCertificate(String receiverCertificate) {
        this.receiverCertificate = receiverCertificate;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
