package com.opuscapita.peppol.validator.controller.validators;

import com.opuscapita.peppol.commons.container.ContainerMessage;
import com.opuscapita.peppol.commons.container.state.log.DocumentValidationError;
import com.opuscapita.peppol.validator.controller.ValidationArtifactsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.xml.validation.Schema;
import java.io.File;

@Component
public class HeaderValidator {

    private final static Logger logger = LoggerFactory.getLogger(HeaderValidator.class);

    private final GenericValidator validator;
    private final ValidationArtifactsRepository repository;

    @Value("${peppol.validator.sbdh.xsdplus}")
    private String xsdPath;

    @Autowired
    public HeaderValidator(GenericValidator validator, ValidationArtifactsRepository repository) {
        this.validator = validator;
        this.repository = repository;
    }

    @PostConstruct
    private void checkValues() {
        File file = new File(getClass().getResource(ValidationArtifactsRepository.RULES_ROOT + xsdPath).getFile());
        if (!file.exists()) {
            throw new IllegalArgumentException("Required file not found: " + file.getAbsolutePath());
        }
    }

    public ContainerMessage validate(byte[] data, ContainerMessage cm) throws SAXException {
        if (data.length == 0) {
            return cm;
        }

        logger.debug("Running check " + xsdPath + " against " + cm.getFileName());
        Schema schema = repository.getArtifactAsSchema(xsdPath);
        DocumentValidationError error = validator.validate(data, schema);

        if (error != null) {
            cm.getHistory().addValidationError(error);
        }
        return cm;
    }

}
