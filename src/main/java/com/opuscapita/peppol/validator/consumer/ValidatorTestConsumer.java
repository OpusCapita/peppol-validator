package com.opuscapita.peppol.validator.consumer;

import com.opuscapita.peppol.commons.container.ContainerMessage;
import com.opuscapita.peppol.commons.container.metadata.ContainerMessageMetadata;
import com.opuscapita.peppol.commons.container.metadata.MetadataExtractor;
import com.opuscapita.peppol.validator.controller.document.DocumentSplitter;
import com.opuscapita.peppol.validator.controller.document.DocumentSplitterResult;
import com.opuscapita.peppol.validator.controller.validators.HeaderValidator;
import com.opuscapita.peppol.validator.controller.validators.PayloadValidator;
import com.opuscapita.peppol.validator.rest.dto.ValidationRestResponse;
import com.opuscapita.peppol.validator.rule.SvefakturaRuleExceptions;
import com.opuscapita.peppol.validator.rule.ValidationRule;
import com.opuscapita.peppol.validator.rule.ValidationRuleConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
@Component
public class ValidatorTestConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ValidatorTestConsumer.class);

    private ValidationRuleConfig ruleConfig;
    private HeaderValidator headerValidator;
    private PayloadValidator payloadValidator;
    private DocumentSplitter documentSplitter;
    private MetadataExtractor metadataExtractor;

    @Autowired
    public ValidatorTestConsumer(HeaderValidator headerValidator, PayloadValidator payloadValidator,
                                 ValidationRuleConfig ruleConfig, DocumentSplitter documentSplitter,
                                 MetadataExtractor metadataExtractor) {
        this.ruleConfig = ruleConfig;
        this.headerValidator = headerValidator;
        this.payloadValidator = payloadValidator;
        this.documentSplitter = documentSplitter;
        this.metadataExtractor = metadataExtractor;
    }

    public ValidationRestResponse consume(File file) throws Exception {
        String filename = file.getName();
        ValidationRestResponse response = new ValidationRestResponse();

        logger.debug("Extracting metadata of the message: " + filename);
        ContainerMessageMetadata metadata = extractMetadataFromHeader(file);
        if (metadata == null) {
            metadata = extractMetadataFromPayload(file);
        }
        if (metadata == null) {
            response.addMessage("Could not extract the metadata from file");
            return response;
        }

        logger.debug("Validating metadata of the message: " + filename);
        String validationResult = validateMetadata(metadata);
        if (validationResult != null) {
            response.addMessage(validationResult);
            return response;
        }

        logger.debug("Getting validation rule of the message: " + filename);
        ValidationRule rule = ruleConfig.getRule(metadata);
        if (rule == null) {
            response.addMessage("Cannot find a validation artifact for file");
            return response;
        }
        response.setRule(rule.convert());

        logger.debug("Split the content, and start actual rule validation");
        try {
            // creating a temp container message to collect validation errors
            ContainerMessage cm = new ContainerMessage(file.getName());

            DocumentSplitterResult parts = splitDocument(file, rule);
            cm = headerValidator.validate(parts.getHeader(), cm);
            cm = payloadValidator.validate(parts.getBody(), cm, rule);
            if (parts.getAttachmentError() != null) {
                cm.getHistory().addValidationError(parts.getAttachmentError());
            }

            // Svefaktura v1+Attachment with Envelope
            if (response.getRule().getId() == 29) {
                SvefakturaRuleExceptions.except(cm);
            }

            // dumping logs from container message to rest response
            response.getMessages().addAll(cm.getHistory().getLogs());

        } catch (Exception e) {
            response.addMessage(e.getMessage());
        }

        logger.debug("Test validation finished");
        return response;
    }

    private DocumentSplitterResult splitDocument(File file, ValidationRule rule) throws IOException, XMLStreamException {
        try {
            try (InputStream content = new FileInputStream(file)) {
                return documentSplitter.split(content, rule);
            }
        } catch (XMLStreamException e) {
            logger.warn("Document Splitter exception for file: " + file.getName() + ", reason: " + e.getMessage());

            try (InputStream content = new FileInputStream(file)) {
                logger.debug("Probably the file is marked as UTF8 but includes non-UTF8 chars.");
                return documentSplitter.split(content, rule, "ISO-8859-1");
            }
        }
    }

    private ContainerMessageMetadata extractMetadataFromHeader(File file) throws Exception {
        try (InputStream content = new FileInputStream(file)) {
            return metadataExtractor.extract(content);
        }
    }

    private ContainerMessageMetadata extractMetadataFromPayload(File file) throws Exception {
        try (InputStream content = new FileInputStream(file)) {
            return metadataExtractor.extractFromPayload(content);
        }
    }

    private String validateMetadata(ContainerMessageMetadata metadata) {
        List<String> missingFields = new ArrayList<>();
        if (StringUtils.isBlank(metadata.getMessageId())) {
            missingFields.add("messageId");
        }
        if (StringUtils.isBlank(metadata.getTransmissionId())) {
            missingFields.add("transmissionId");
        }
        if (StringUtils.isBlank(metadata.getSenderId())) {
            missingFields.add("senderId");
        }
        if (StringUtils.isBlank(metadata.getRecipientId())) {
            missingFields.add("receiverId");
        }
        if (StringUtils.isBlank(metadata.getDocumentTypeIdentifier())) {
            missingFields.add("documentTypeIdentifier");
        }
        if (StringUtils.isBlank(metadata.getProfileTypeIdentifier())) {
            missingFields.add("profileTypeIdentifier");
        }

        if (!missingFields.isEmpty()) {
            String tmp = missingFields.stream().collect(Collectors.joining(", "));
            return "Missing metadata information [" + tmp + "]";
        }
        return null;
    }
}
