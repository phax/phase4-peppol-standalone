/*
 * Copyright (C) 2023-204 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mysupply.phase4.peppolstandalone.dto;

import com.mysupply.phase4.domain.Document;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for displaying document information without the actual data content.
 * Used for admin overview purposes.
 */
public class DocumentOverview {
    private UUID id;
    private OffsetDateTime created;
    private String domain;
    private String senderIdentifier;
    private String receiverIdentifier;
    private String docType;
    private String process;
    private String protocol;
    private String conversationId;
    private String messageId;
    private OffsetDateTime retrieved;
    private UUID vaxId;
    private String retrievedByInstanceName;
    private UUID retrievedByConnectorId;
    private String retrievedByConnectorName;
    private long dataSize;

    public DocumentOverview() {
    }

    /**
     * Constructor for JPQL projection - used by repository query to avoid fetching the data blob.
     */
    public DocumentOverview(UUID id, OffsetDateTime created, String domain, String senderIdentifier,
                           String receiverIdentifier, String docType, String process, String protocol,
                           String conversationId, String messageId, OffsetDateTime retrieved, UUID vaxId,
                           String retrievedByInstanceName, UUID retrievedByConnectorId,
                           String retrievedByConnectorName, long dataSize) {
        this.id = id;
        this.created = created;
        this.domain = domain;
        this.senderIdentifier = senderIdentifier;
        this.receiverIdentifier = receiverIdentifier;
        this.docType = docType;
        this.process = process;
        this.protocol = protocol;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.retrieved = retrieved;
        this.vaxId = vaxId;
        this.retrievedByInstanceName = retrievedByInstanceName;
        this.retrievedByConnectorId = retrievedByConnectorId;
        this.retrievedByConnectorName = retrievedByConnectorName;
        this.dataSize = dataSize;
    }

    public DocumentOverview(Document document) {
        this.id = document.getId();
        this.created = document.getCreated();
        this.domain = document.getDomain();
        this.senderIdentifier = document.getSenderIdentifier();
        this.receiverIdentifier = document.getReceiverIdentifier();
        this.docType = document.getDocType();
        this.process = document.getProcess();
        this.protocol = document.getProtocol();
        this.conversationId = document.getConversationId();
        this.messageId = document.getMessageId();
        this.retrieved = document.getRetrieved();
        this.vaxId = document.getVaxId();
        this.retrievedByInstanceName = document.getRetrievedByInstanceName();
        this.retrievedByConnectorId = document.getRetrievedByConnectorId();
        this.retrievedByConnectorName = document.getRetrievedByConnectorName();
        this.dataSize = document.getData() != null ? document.getData().length : 0;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public OffsetDateTime getRetrieved() {
        return retrieved;
    }

    public void setRetrieved(OffsetDateTime retrieved) {
        this.retrieved = retrieved;
    }

    public UUID getVaxId() {
        return vaxId;
    }

    public void setVaxId(UUID vaxId) {
        this.vaxId = vaxId;
    }

    public String getRetrievedByInstanceName() {
        return retrievedByInstanceName;
    }

    public void setRetrievedByInstanceName(String retrievedByInstanceName) {
        this.retrievedByInstanceName = retrievedByInstanceName;
    }

    public UUID getRetrievedByConnectorId() {
        return retrievedByConnectorId;
    }

    public void setRetrievedByConnectorId(UUID retrievedByConnectorId) {
        this.retrievedByConnectorId = retrievedByConnectorId;
    }

    public String getRetrievedByConnectorName() {
        return retrievedByConnectorName;
    }

    public void setRetrievedByConnectorName(String retrievedByConnectorName) {
        this.retrievedByConnectorName = retrievedByConnectorName;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    /**
     * Returns true if this document has been retrieved (processed).
     */
    public boolean isRetrieved() {
        return this.retrieved != null;
    }

    /**
     * Returns a status string for display purposes.
     */
    public String getStatus() {
        return this.retrieved != null ? "Retrieved" : "Pending";
    }
}

