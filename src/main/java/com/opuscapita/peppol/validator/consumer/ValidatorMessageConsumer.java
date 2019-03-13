package com.opuscapita.peppol.validator.consumer;

import com.opuscapita.peppol.commons.container.ContainerMessage;
import com.opuscapita.peppol.commons.container.state.Endpoint;
import com.opuscapita.peppol.commons.container.state.ProcessType;
import com.opuscapita.peppol.commons.container.state.Route;
import com.opuscapita.peppol.commons.eventing.EventReporter;
import com.opuscapita.peppol.commons.eventing.TicketReporter;
import com.opuscapita.peppol.commons.queue.MessageQueue;
import com.opuscapita.peppol.commons.queue.consume.ContainerMessageConsumer;
import com.opuscapita.peppol.commons.storage.Storage;
import com.opuscapita.peppol.validator.controller.ContainerMetadataValidator;
import com.opuscapita.peppol.validator.controller.document.DocumentSplitter;
import com.opuscapita.peppol.validator.controller.document.DocumentSplitterResult;
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

    @Value("${spring.application.name}")
    private String componentName;

    private Storage storage;
    private MessageQueue messageQueue;
    private EventReporter eventReporter;
    private TicketReporter ticketReporter;
    private DocumentSplitter documentSplitter;
    private ContainerMetadataValidator metadataValidator;

    @Autowired
    public ValidatorMessageConsumer(Storage storage, MessageQueue messageQueue, EventReporter eventReporter, TicketReporter ticketReporter,
                                    DocumentSplitter documentSplitter, ContainerMetadataValidator metadataValidator) {
        this.storage = storage;
        this.messageQueue = messageQueue;
        this.eventReporter = eventReporter;
        this.ticketReporter = ticketReporter;
        this.documentSplitter = documentSplitter;
        this.metadataValidator = metadataValidator;
    }

    @Override
    public void consume(@NotNull ContainerMessage cm) throws Exception {
        cm.setStatus(getCurrentEndpoint(cm), "received");
        logger.info("Validator received the message: " + cm.toLog());

        if (StringUtils.isBlank(cm.getFileName())) {
            throw new IllegalArgumentException("File name is empty in received message: " + cm.toLog());
        }

        logger.info("Checking metadata of the message: " + cm.getFileName());
        metadataValidator.validate(cm);
        if (StringUtils.isNotBlank(cm.getProcessingException())) {
            cm.setCurrentStatus("validation error: invalid metadata");
            logger.info("Validation failed for the message: " + cm.toLog() + " reason: " + cm.getProcessingException());
            eventReporter.reportStatus(cm);
            ticketReporter.reportWithContainerMessage(cm, null, "Validation failed for the message: " + cm.getFileName());
            return;
        }

        InputStream content = storage.get(cm.getFileName());


//        logger.info("Loading route info for the message: " + cm.getFileName());
//        Route route = messageRouter.loadRoute(cm);
//        cm.setCurrentStatus("route info set");
//        cm.setRoute(route);
//
//        if (StringUtils.isNotBlank(cm.getProcessingException())) {
//            cm.setCurrentStatus("processing error: invalid route");
//            logger.info("Processing failed for the message: " + cm.toLog() + " reason: " + cm.getProcessingException());
//            eventReporter.reportStatus(cm);
//            ticketReporter.reportWithContainerMessage(cm, null, "Processing failed for the message: " + cm.getFileName());
//            return;
//        }
//
//        String outputQueue = cm.popRoute();
//        messageQueue.convertAndSend(outputQueue, cm);
//        logger.info("The message: " + cm.toLog() + " successfully validated and delivered to " + outputQueue + " queue");
    }

    private Endpoint getCurrentEndpoint(@NotNull ContainerMessage cm) {
        return new Endpoint(componentName, (cm.isInbound() ? ProcessType.IN_VALIDATION : ProcessType.OUT_VALIDATION));
    }

}
