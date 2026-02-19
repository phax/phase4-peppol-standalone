package com.mysupply.phase4.peppolstandalone.dto;

import java.util.UUID;

public class ConfirmSetting {

    private UUID documentId;
    private String connectorName;
    private UUID connectorId;
    private String instanceName;
    private UUID vaxId;

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public UUID getConnectorId() {
        return connectorId;
    }

    public void setConnectorName(UUID connectorId) {
        this.connectorId = connectorId;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public UUID getVaxId() {
        return vaxId;
    }

    public void setVaxId(UUID vaxId) {
        this.vaxId = vaxId;
    }
}
