package com.mysupply.phase4.peppolstandalone.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LookupReport {
    private boolean lookupCompleted;
    private boolean receiverExist;
    private String errorMessage;

    public boolean isLookupCompleted() {
        return lookupCompleted;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setLookupCompleted(boolean lookupCompleted) {
        this.lookupCompleted = lookupCompleted;
    }

    public boolean isReceiverExist() {
        return receiverExist;
    }

    public void setReceiverExist(boolean receiverExist) {
        this.receiverExist = receiverExist;
    }

    public String convertToJsonString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"lookupCompleted\":false,\"receiverExist\":false,\"errorMessage\":\"" + e.getMessage() + "\"}";
        }
    }
}
