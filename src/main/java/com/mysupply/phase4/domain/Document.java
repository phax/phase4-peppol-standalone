package com.mysupply.phase4.domain;

import com.mysupply.phase4.persistence.DocumentConstants;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = DocumentConstants.DOCUMENT_TABLE_NAME, schema = DocumentConstants.DOCUMENT_SCHEMA_NAME)
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private byte[] data;
    private OffsetDateTime created;
    private String domain;

    @Column(name = "sender_identifier")
    private String senderIdentifier;

    @Column(name = "receiver_identifier")
    private String receiverIdentifier;

    @Column(name = "doc_type")
    private String docType;

    private String process;

    @Column(name = "sender_certificate")
    private String senderCertificate;

    @Column(name = "receiver_certificate")
    private String receiverCertificate;

    @Column(name = "protocol")
    private String protocol;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "message_id")
    private String messageId;

    //private DocumentStatus documentStatus;
    private OffsetDateTime retrieved;

    @Column(name = "vax_id")
    private UUID vaxId;

    @Column(name = "retrieved_by_instance_name")
    private String retrievedByInstanceName;

    @Column(name = "retrieved_by_connector_id")
    private UUID retrievedByConnectorId;

    @Column(name = "retrieved_by_connector_name")
    private String retrievedByConnectorName;

    @Column(name = "data_size")
    private long dataSize;

    protected Document() {
    }

    private Document(Builder builder) {
        this.data = builder.data;
        this.dataSize = builder.dataSize;
        this.domain = builder.domain;
        this.senderIdentifier = builder.senderIdentifier;
        this.receiverIdentifier = builder.receiverIdentifier;
        this.docType = builder.docType;
        this.process = builder.process;
        this.senderCertificate = builder.senderCertificate;
        this.receiverCertificate = builder.receiverCertificate;
        this.protocol = builder.protocol;
        this.conversationId = builder.conversationId;
        this.messageId = builder.messageId;
        this.created = OffsetDateTime.now(ZoneOffset.UTC);
        //this.documentStatus = DocumentStatus.Created;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private byte[] data;
        private long dataSize;
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

        public Builder data(byte[] data) {
            this.data = data;
            return this;
        }

        public Builder dataSize(long dataSize) {
            this.dataSize = dataSize;
            return this;
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder senderIdentifier(String senderIdentifier) {
            this.senderIdentifier = senderIdentifier;
            return this;
        }

        public Builder receiverIdentifier(String receiverIdentifier) {
            this.receiverIdentifier = receiverIdentifier;
            return this;
        }

        public Builder docType(String docType) {
            this.docType = docType;
            return this;
        }

        public Builder process(String process) {
            this.process = process;
            return this;
        }

        public Builder senderCertificate(String senderCertificate) {
            this.senderCertificate = senderCertificate;
            return this;
        }

        public Builder receiverCertificate(String receiverCertificate) {
            this.receiverCertificate = receiverCertificate;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Document build() {
            return new Document(this);
        }
    }

    // Getters
    public UUID getId() {
        return this.id;
    }

    public byte[] getData() {
        return data;
    }

    public long getDataSize() {
        return this.dataSize;
    }

    public OffsetDateTime getCreated() {
        return this.created;
    }

    public OffsetDateTime getRetrieved() {
        return this.retrieved;
    }

    public String getDomain() {
        return this.domain;
    }

    public String getSenderIdentifier() {
        return this.senderIdentifier;
    }

    public String getReceiverIdentifier() {
        return this.receiverIdentifier;
    }

    public String getDocType() {
        return this.docType;
    }

    public String getProcess() {
        return this.process;
    }

    public String getSenderCertificate() {
        return this.senderCertificate;
    }

    public String getReceiverCertificate() {
        return this.receiverCertificate;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getConversationId() {
        return this.conversationId;
    }

    public String getMessageId() {
        return this.messageId;
    }

    public String getRetrievedByInstanceName() { return this.retrievedByInstanceName; }

    public UUID getRetrievedByConnectorId() {
        return this.retrievedByConnectorId;
    }

    public String getRetrievedByConnectorName() {
        return this.retrievedByConnectorName;
    }

   // public DocumentStatus getDocumentStatus() { return this.documentStatus; }

    // Setters
    public void setData(byte[] data) {

        this.data = data;

    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public void setRetrieved(OffsetDateTime retrieved) {
        this.retrieved = retrieved;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setSenderIdentifier(String senderIdentifier) {
        this.senderIdentifier = senderIdentifier;
    }

    public void setReceiverIdentifier(String receiverIdentifier) {
        this.receiverIdentifier = receiverIdentifier;
    }

    public void setDocType(String docType) { this.docType = docType; }

    public void setProcess(String process) {
        this.process = process;
    }

    public void setSenderCertificate(String senderCertificate) {
        this.senderCertificate = senderCertificate;
    }

    public void setReceiverCertificate(String receiverCertificate) {
        this.receiverCertificate = receiverCertificate;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setRetrievedByInstance(String retrievedByInstanceName) {
        this.retrievedByInstanceName = retrievedByInstanceName;
    }

    public void setRetrievedByConnectorId(UUID retrievedByConnectorId) {
        this.retrievedByConnectorId = retrievedByConnectorId;
    }

    public void setRetrievedByConnectorName(String retrievedByConnectorName) {
        this.retrievedByConnectorName = retrievedByConnectorName;
    }


    public UUID getVaxId() {
        return vaxId;
    }

    public void setVaxId(UUID vaxId) {
        this.vaxId = vaxId;
    }

  //  public void getDocumentStatus(DocumentStatus documentStatus) { this.documentStatus = documentStatus; }
}
