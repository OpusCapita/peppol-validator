package com.opuscapita.peppol.validator.consumer;

import com.opuscapita.peppol.commons.container.ContainerMessage;
import com.opuscapita.peppol.commons.container.metadata.MetadataExtractor;
import com.opuscapita.peppol.commons.container.state.Endpoint;
import com.opuscapita.peppol.commons.container.state.ProcessFlow;
import com.opuscapita.peppol.commons.container.state.ProcessStep;
import com.opuscapita.peppol.commons.container.state.Source;
import com.opuscapita.peppol.validator.controller.document.DocumentSplitter;
import com.opuscapita.peppol.validator.controller.document.DocumentSplitterResult;
import com.opuscapita.peppol.validator.controller.validators.HeaderValidator;
import com.opuscapita.peppol.validator.controller.validators.PayloadValidator;
import com.opuscapita.peppol.validator.rule.ValidationRule;
import com.opuscapita.peppol.validator.rule.ValidationRuleConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@EnableAutoConfiguration
public class ValidationProcessTest {

    private static final Logger logger = LoggerFactory.getLogger(ValidationProcessTest.class);

    @Autowired
    MetadataExtractor metadataExtractor;
    @Autowired
    ValidationRuleConfig ruleConfig;
    @Autowired
    DocumentSplitter documentSplitter;
    @Autowired
    HeaderValidator headerValidator;
    @Autowired
    PayloadValidator payloadValidator;

    @Test
    @Ignore
    public void testSingleFile() throws Exception {
        File file = ResourceUtils.getFile("classpath:test-materials/sample-file-to-peppol.xml");
        Endpoint endpoint = new Endpoint(Source.UNKNOWN, ProcessStep.TEST);
        ContainerMessage cm = new ContainerMessage(file.getName(), endpoint);

        try (InputStream inputStream = new FileInputStream(file)) {
            cm.setMetadata(metadataExtractor.extract(inputStream));
        }

        ValidationRule rule = ruleConfig.getRule(cm);

        DocumentSplitterResult parts;
        try (InputStream inputStream = new FileInputStream(file)) {
            parts = documentSplitter.split(inputStream, rule);
        }

        cm = headerValidator.validate(parts.getHeader(), cm);
        cm = payloadValidator.validate(parts.getBody(), cm, rule);

        if (cm.getHistory().hasError()) {
            logger.info("Validation failed for the file: " + file.getName());
        } else {
            logger.info("Validation successful for the file: " + file.getName());
        }
    }

}

