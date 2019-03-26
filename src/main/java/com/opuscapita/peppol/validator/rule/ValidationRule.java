package com.opuscapita.peppol.validator.rule;

import com.opuscapita.peppol.commons.container.ContainerMessage;
import com.opuscapita.peppol.commons.container.metadata.PeppolMessageMetadata;

import java.util.List;

public class ValidationRule {

    private Integer id;
    private String description;
    private String archetype;
    private String localName;
    private String documentId;
    private String processId;
    private String processSchema;
    private String version;
    private List<String> rules;
    private List<String> suppress;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getArchetype() {
        return archetype;
    }

    public void setArchetype(String archetype) {
        this.archetype = archetype;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getProcessSchema() {
        return processSchema;
    }

    public void setProcessSchema(String processSchema) {
        this.processSchema = processSchema;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getRules() {
        return rules;
    }

    public void setRules(List<String> rules) {
        this.rules = rules;
    }

    public List<String> getSuppress() {
        return suppress;
    }

    public void setSuppress(List<String> suppress) {
        this.suppress = suppress;
    }

    public boolean matches(ContainerMessage cm) {
        PeppolMessageMetadata metadata = cm.getMetadata();
        if (metadata == null || metadata.getDocumentTypeIdentifier() == null || metadata.getProfileTypeIdentifier() == null) {
            return false;
        }

        return metadata.getDocumentTypeIdentifier().equals(documentId) && metadata.getProfileTypeIdentifier().equals(processId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValidationRule that = (ValidationRule) o;

        if (documentId != null && processId != null) {
            return documentId.equals(that.documentId) && processId.equals(that.processId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = documentId.hashCode();
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("ValidationRule{id: %s, name: %s}", id, description);
    }

}
