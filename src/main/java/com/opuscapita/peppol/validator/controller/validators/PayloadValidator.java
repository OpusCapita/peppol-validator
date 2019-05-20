package com.opuscapita.peppol.validator.controller.validators;

import com.opuscapita.peppol.commons.container.ContainerMessage;
import com.opuscapita.peppol.commons.container.state.log.DocumentValidationError;
import com.opuscapita.peppol.validator.controller.ValidationArtifactsRepository;
import com.opuscapita.peppol.validator.controller.parser.ValidationResultParser;
import com.opuscapita.peppol.validator.rule.ValidationRule;
import net.sf.saxon.trans.XPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;
import org.xml.sax.SAXException;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.validation.Schema;
import java.io.IOException;

@Component
public class PayloadValidator {

    private final static Logger logger = LoggerFactory.getLogger(PayloadValidator.class);

    private final GenericValidator validator;
    private final ValidationResultParser resultParser;
    private final ValidationArtifactsRepository repository;

    @Autowired
    public PayloadValidator(GenericValidator validator, ValidationResultParser resultParser, ValidationArtifactsRepository repository) {
        this.validator = validator;
        this.repository = repository;
        this.resultParser = resultParser;
    }

    public ContainerMessage validate(byte[] data, ContainerMessage cm, ValidationRule rule) throws TransformerException, IOException, SAXException {
        for (String file : rule.getRules()) {
            logger.debug("Running check " + file + " against " + cm.getFileName());

            if (file.toLowerCase().endsWith(".xsl")) {
                Templates template = repository.getArtifactAsTemplate(file);
                cm = executeXsl(data, cm, template, rule, file);
            } else if (file.toLowerCase().endsWith(".xsd")) {
                Schema schema = repository.getArtifactAsSchema(file);
                cm = executeXsd(data, cm, schema);
            } else {
                logger.warn("Ignoring unknown rule format: " + file + " for file " + cm.getFileName() +
                        "(" + rule.getDescription() + "), supported formats are 'xsl' and 'xsd'");
            }
        }

        return cm;
    }

    private ContainerMessage executeXsd(byte[] data, ContainerMessage cm, Schema schema) {
        DocumentValidationError error = validator.validate(data, schema);
        if (error != null) {
            cm.getHistory().addValidationError(error);
        }
        return cm;
    }

    private ContainerMessage executeXsl(byte[] data, ContainerMessage cm, Templates template, ValidationRule rule, String file) throws TransformerException, IOException, SAXException {
        try {
            FastByteArrayOutputStream rawResult = validator.validate(data, template);
            return resultParser.parse(rawResult.getInputStream(), cm, rule);
        } catch (XPathException e) {
            DocumentValidationError validationError = new DocumentValidationError("XSL Parser Failure")
                    .withLocation(e.getLocator() == null ? "Undefined location" : e.getLocator().toString())
                    .withText(e.getMessage())
                    .withIdentifier(e.getErrorCodeQName() == null ? "ERR-UNKNOWN" : e.getErrorCodeQName().toString())
                    .withFlag("FATAL")
                    .withTest("XSL Validation: " + file);
            cm.getHistory().addValidationError(validationError);
            return cm;
        }
    }

}
