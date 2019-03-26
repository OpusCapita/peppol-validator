package com.opuscapita.peppol.validator.consumer;

import com.opuscapita.peppol.commons.container.ContainerMessage;
import com.opuscapita.peppol.commons.container.state.ProcessStep;
import com.opuscapita.peppol.commons.eventing.EventReporter;
import com.opuscapita.peppol.commons.eventing.TicketReporter;
import com.opuscapita.peppol.commons.queue.MessageQueue;
import com.opuscapita.peppol.commons.queue.consume.ContainerMessageConsumer;
import com.opuscapita.peppol.commons.storage.Storage;
import com.opuscapita.peppol.validator.controller.document.DocumentSplitter;
import com.opuscapita.peppol.validator.controller.document.DocumentSplitterResult;
import com.opuscapita.peppol.validator.controller.validators.HeaderValidator;
import com.opuscapita.peppol.validator.controller.validators.MetadataValidator;
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
                                    ValidationRuleConfig ruleConfig, DocumentSplitter documentSplitter, MetadataValidator metadataValidator) {
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
        cm.setStep(ProcessStep.VALIDATION);
        cm.getHistory().addInfo("Received and started validating");
        logger.info("Validator received the message: " + cm.toKibana());

        if (StringUtils.isBlank(cm.getFileName())) {
            throw new IllegalArgumentException("File name is empty in received message: " + cm.toKibana());
        }

        logger.info("Checking metadata of the message: " + cm.getFileName());
        metadataValidator.validate(cm);
        if (cm.getHistory().hasError()) {
            logger.info("Validation failed for the message: " + cm.toKibana() + " reason: " + cm.getHistory().getLastLog().getMessage());
            cm.getHistory().addInfo("Validation failed: invalid metadata");

            eventReporter.reportStatus(cm);
            ticketReporter.reportWithContainerMessage(cm, null, "Validation failed for the message: " + cm.getFileName());
            return;
        }

        logger.debug("Getting validation rule of the message: " + cm.getFileName());
        ValidationRule rule = ruleConfig.getRule(cm);
        if (rule == null) {
            logger.info("Validation failed for the message: " + cm.toKibana() + " reason: " + cm.getHistory().getLastLog().getMessage());
            cm.getHistory().addInfo("Validation failed: no rule found");

            eventReporter.reportStatus(cm);
            messageQueue.convertAndSend(emailSenderQueue, cm);
            ticketReporter.reportWithContainerMessage(cm, null, "Validation failed for the message: " + cm.getFileName());
            return;
        }
        logger.info(rule.toString() + " found for the message: " + cm.getFileName());

        InputStream content = storage.get(cm.getFileName());
        DocumentSplitterResult parts = documentSplitter.split(content, rule);

        cm = headerValidator.validate(parts.getHeader(), cm);
        cm = payloadValidator.validate(parts.getBody(), cm, rule);

        if (parts.getAttachmentError() != null) {
            cm.getHistory().addValidationError(parts.getAttachmentError());
        }

        if (cm.getHistory().hasError()) {
            cm.getHistory().addInfo("Validation failed: invalid file");

            eventReporter.reportStatus(cm);
            messageQueue.convertAndSend(emailSenderQueue, cm);
            logger.info("Validation failed for " + cm.toKibana() + ", message sent to " + emailSenderQueue + " queue");
            return;
        }

        messageQueue.convertAndSend(queueOut, cm);
        cm.getHistory().addInfo("Validation completed successfully");
        logger.info("The message: " + cm.toKibana() + " successfully validated and delivered to " + queueOut + " queue");
    }

}
