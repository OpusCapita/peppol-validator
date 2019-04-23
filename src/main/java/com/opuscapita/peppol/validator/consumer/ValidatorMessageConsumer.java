package com.opuscapita.peppol.validator.consumer;

import com.opuscapita.peppol.commons.container.ContainerMessage;
import com.opuscapita.peppol.commons.container.metadata.MetadataValidator;
import com.opuscapita.peppol.commons.container.state.ProcessStep;
import com.opuscapita.peppol.commons.eventing.EventReporter;
import com.opuscapita.peppol.commons.eventing.TicketReporter;
import com.opuscapita.peppol.commons.queue.MessageQueue;
import com.opuscapita.peppol.commons.queue.consume.ContainerMessageConsumer;
import com.opuscapita.peppol.commons.storage.Storage;
import com.opuscapita.peppol.validator.controller.document.DocumentSplitter;
import com.opuscapita.peppol.validator.controller.document.DocumentSplitterResult;
import com.opuscapita.peppol.validator.controller.validators.HeaderValidator;
import com.opuscapita.peppol.validator.controller.validators.PayloadValidator;
import com.opuscapita.peppol.validator.rule.ValidationRule;
import com.opuscapita.peppol.validator.rule.ValidationRuleConfig;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;

@Component
public class ValidatorMessageConsumer implements ContainerMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ValidatorMessageConsumer.class);

    @Value("${peppol.validator.queue.out.name}")
    private String queueOut;

    @Value("${peppol.email-sender.queue.in.name}")
    private String emailSenderQueue;

    private Storage storage;
    private MessageQueue messageQueue;
    private EventReporter eventReporter;
    private TicketReporter ticketReporter;
    private ValidationRuleConfig ruleConfig;
    private HeaderValidator headerValidator;
    private PayloadValidator payloadValidator;
    private DocumentSplitter documentSplitter;
    private MetadataValidator metadataValidator;

    @Autowired
    public ValidatorMessageConsumer(Storage storage, MessageQueue messageQueue,
                                    EventReporter eventReporter, TicketReporter ticketReporter,
                                    HeaderValidator headerValidator, PayloadValidator payloadValidator,
                                    ValidationRuleConfig ruleConfig, DocumentSplitter documentSplitter,
                                    MetadataValidator metadataValidator) {
        this.storage = storage;
        this.ruleConfig = ruleConfig;
        this.messageQueue = messageQueue;
        this.eventReporter = eventReporter;
        this.ticketReporter = ticketReporter;
        this.headerValidator = headerValidator;
        this.payloadValidator = payloadValidator;
        this.documentSplitter = documentSplitter;
        this.metadataValidator = metadataValidator;
    }

    @Override
    public void consume(@NotNull ContainerMessage cm) throws Exception {
        cm.setStep(ProcessStep.VALIDATOR);
        cm.getHistory().addInfo("Received and started validating");
        logger.info("Validator received the message: " + cm.toKibana());

        if (StringUtils.isBlank(cm.getFileName())) {
            throw new IllegalArgumentException("File name is empty in received message: " + cm.toKibana());
        }

        logger.debug("Checking metadata of the message: " + cm.getFileName());
        metadataValidator.validate(cm);
        if (cm.getHistory().hasError()) {
            String shortDescription = "Validation failed for '" + cm.getFileName() + "' reason: " + cm.getHistory().getLastLog().getMessage();
            logger.info(shortDescription);
            cm.getHistory().addInfo("Validation failed: invalid metadata");

            eventReporter.reportStatus(cm);
            ticketReporter.reportWithContainerMessage(cm, null, shortDescription);
            return;
        }

        logger.debug("Getting validation rule of the message: " + cm.getFileName());
        ValidationRule rule = ruleConfig.getRule(cm);
        if (rule == null) {
            String shortDescription = "Validation failed for '" + cm.getFileName() + "' reason: " + cm.getHistory().getLastLog().getMessage();
            logger.info(shortDescription);
            cm.getHistory().addInfo("Validation failed: no rule found");

            eventReporter.reportStatus(cm);
            messageQueue.convertAndSend(emailSenderQueue, cm);
            ticketReporter.reportWithContainerMessage(cm, null, shortDescription);
            return;
        }
        cm.getHistory().addInfo("Found rule: " + rule.toString());
        logger.info(rule.toString() + " found for the message: " + cm.getFileName());

        logger.debug("Read the file content, split it, and start actual rule validation");
        try {
            DocumentSplitterResult parts = splitDocument(cm, rule);

            cm = headerValidator.validate(parts.getHeader(), cm);
            cm = payloadValidator.validate(parts.getBody(), cm, rule);

            if (parts.getAttachmentError() != null) {
                cm.getHistory().addValidationError(parts.getAttachmentError());
            }

        } catch (Exception e) {
            String shortDescription = "Validation failed for '" + cm.getFileName() + "' reason: " + e.getMessage();
            logger.info(shortDescription);

            cm.getHistory().addError(e.getMessage());
            cm.getHistory().addInfo("Validation failed: unexpected error");

            eventReporter.reportStatus(cm);
            ticketReporter.reportWithContainerMessage(cm, e, shortDescription);
            return;
        }

        logger.debug("Validation finished, checking for any errors");
        if (cm.getHistory().hasError()) {
            cm.getHistory().addInfo("Validation failed: invalid file");

            eventReporter.reportStatus(cm);
            messageQueue.convertAndSend(emailSenderQueue, cm);
            logger.info("Validation failed for " + cm.toKibana() + ", message sent to " + emailSenderQueue + " queue");
            return;
        }

        cm.getHistory().addInfo("Validation completed successfully");
        logger.info("The message: " + cm.toKibana() + " successfully validated and delivered to " + queueOut + " queue");
        eventReporter.reportStatus(cm);
        messageQueue.convertAndSend(queueOut, cm);
    }

    private DocumentSplitterResult splitDocument(ContainerMessage cm, ValidationRule rule) throws IOException, XMLStreamException {
        try {
            try (InputStream content = storage.get(cm.getFileName())) {
                return documentSplitter.split(content, rule);
            }
        } catch (XMLStreamException e) {
            logger.warn("Document Splitter exception for file: " + cm.getFileName() + ", reason: " + e.getMessage());

            try (InputStream content = storage.get(cm.getFileName())) {
                logger.debug("Probably the file is marked as UTF8 but includes non-UTF8 chars.");
                return documentSplitter.split(content, rule, "ISO-8859-1");
            }
        }
    }

}
