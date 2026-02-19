package com.mysupply.phase4.peppolstandalone.dto;

import java.util.List;

/**
 * Settings for retrieving messages from Phase Four service.
 */
public class RetrieveSearchSetting {
    private List<String> senderIdentifiers;

    /**
     * When true, the SenderIdentifiers list is ignored and all senders are retrieved.
     */
    private boolean retrieveFromAllSenders;

    private List<String> receiverIdentifiers;

    /**
     * When true, the ReceiverIdentifiers list is ignored and all receivers are retrieved.
     */
    private boolean retrieveFromAllReceivers;

    private List<String> domains;

    /**
     * When true, the Domains list is ignored and all domains are retrieved.
     */
    private boolean retrieveFromAllDomains;

    public RetrieveSearchSetting() {
    }

    public List<String> getSenderIdentifiers() {
        return this.senderIdentifiers;
    }

    public void setSenderIdentifiers(List<String> senderIdentifiers) {
        this.senderIdentifiers = senderIdentifiers;
    }

    public boolean isRetrieveFromAllSenders() {
        return this.retrieveFromAllSenders;
    }

    public void setRetrieveFromAllSenders(boolean retrieveFromAllSenders) {
        this.retrieveFromAllSenders = retrieveFromAllSenders;
    }

    public List<String> getReceiverIdentifiers() {
        return this.receiverIdentifiers;
    }

    public void setReceiverIdentifiers(List<String> receiverIdentifiers) {
        this.receiverIdentifiers = receiverIdentifiers;
    }

    public boolean isRetrieveFromAllReceivers() {
        return this.retrieveFromAllReceivers;
    }

    public void setRetrieveFromAllReceivers(boolean retrieveFromAllReceivers) {
        this.retrieveFromAllReceivers = retrieveFromAllReceivers;
    }

    public List<String> getDomains() {
        return this.domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    public boolean isRetrieveFromAllDomains() {
        return this.retrieveFromAllDomains;
    }

    public void setRetrieveFromAllDomains(boolean retrieveFromAllDomains) {
        this.retrieveFromAllDomains = retrieveFromAllDomains;
    }
}
