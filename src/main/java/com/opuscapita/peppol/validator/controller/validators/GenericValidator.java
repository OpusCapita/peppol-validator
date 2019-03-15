package com.opuscapita.peppol.validator.controller.validators;

import com.opuscapita.peppol.commons.container.state.log.DocumentValidationError;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.StringReader;

@Component
public class GenericValidator {

    /**
     * Validates byte array data against given XSD schema.
     *
     * @param data   the input data
     * @param schema the XSD schema
     * @return validation error if any or null if there was no XSD error
     */
    public DocumentValidationError validate(byte[] data, Schema schema) {
        Validator validator = schema.newValidator();
        Source source = new StreamSource(new StringReader(new String(data)));

        try {
            validator.validate(source);
        } catch (Exception e) {
            return new DocumentValidationError("XSD validation failure").withText(e.getMessage());
        }

        return null;
    }

    /**
     * Validates byte array data against given XSL template.
     *
     * @param data     the input data
     * @param template the XSL template
     * @return validation result as raw
     */
    public FastByteArrayOutputStream validate(byte[] data, Templates template) throws TransformerException {
        Transformer transformer = template.newTransformer();

        Source source = new StreamSource(new StringReader(new String(data)));
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
        Result result = new StreamResult(outputStream);
        transformer.transform(source, result);

        return outputStream;
    }

}
