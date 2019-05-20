package com.opuscapita.peppol.validator.rest.dto;

import com.opuscapita.peppol.commons.container.metadata.ContainerValidationRule;
import com.opuscapita.peppol.commons.container.state.log.DocumentErrorType;
import com.opuscapita.peppol.commons.container.state.log.DocumentLog;
import com.opuscapita.peppol.commons.container.state.log.DocumentLogLevel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ValidationRestResponse implements Serializable {

    private List<DocumentLog> messages;
    private ContainerValidationRule rule;

    public ValidationRestResponse() {
        this.messages = new ArrayList<>();
    }

    public List<DocumentLog> getMessages() {
        return messages;
    }

    public void setMessages(List<DocumentLog> messages) {
        this.messages = messages;
    }

    public void addMessage(String message) {
        this.addMessage(new DocumentLog(message, DocumentLogLevel.ERROR, DocumentErrorType.PROCESSING_ERROR));
    }

    public void addMessage(DocumentLog message) {
        this.messages.add(message);
    }

    public ContainerValidationRule getRule() {
        return rule;
    }

    public void setRule(ContainerValidationRule rule) {
        this.rule = rule;
    }
}
