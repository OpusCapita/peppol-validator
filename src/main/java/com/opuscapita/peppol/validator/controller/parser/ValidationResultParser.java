package com.opuscapita.peppol.validator.controller.parser;

import com.opuscapita.peppol.commons.container.ContainerMessage;
import com.opuscapita.peppol.commons.container.state.log.DocumentValidationError;
import com.opuscapita.peppol.validator.rule.ValidationRule;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ValidationResultParser {

    private final static Logger logger = LoggerFactory.getLogger(ValidationResultParser.class);

    @Value("${peppol.validator.combine.errors.at:10}")
    private int combineThreshold;

    private SAXParser saxParser;

    public ValidationResultParser(@Lazy SAXParserFactory saxParserFactory) {
        try {
            this.saxParser = saxParserFactory.newSAXParser();
        } catch (SAXException | ParserConfigurationException e) {
            logger.error("Failed to initialize SaxParser", e);
        }
    }

    public ContainerMessage parse(InputStream rawValidationResult, ContainerMessage cm, ValidationRule rule) throws SAXException, IOException {
        // key -> (skipped count, list of records)
        Map<String, MutablePair<Integer, List<DocumentValidationError>>> errorsAndWarnings = new HashMap<>();

        saxParser.parse(rawValidationResult, new DefaultHandler() {
            DocumentValidationError current = null;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                if (attributes.getIndex("test") != -1) {
                    current = parseAttributes(attributes, rule);
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                if (current != null) {
                    String line = new String(ch, start, length);
                    if (StringUtils.isNotBlank(line)) {
                        collect(errorsAndWarnings, current.withText(line));
                        current = null;
                    }
                }
            }
        });

        return flush(cm, errorsAndWarnings);
    }

    private void collect(Map<String, MutablePair<Integer, List<DocumentValidationError>>> errorsAndWarnings, DocumentValidationError record) {
        // new error or warning record of this type
        String key = record.getIdentifier();
        if (errorsAndWarnings.get(key) == null) {
            MutablePair<Integer, List<DocumentValidationError>> pair = new MutablePair<>();
            List<DocumentValidationError> list = new ArrayList<>();
            list.add(record);
            pair.setRight(list);
            pair.setLeft(0);
            errorsAndWarnings.put(key, pair);
            return;
        }

        // such error is known but there are not so many of them to combine
        MutablePair<Integer, List<DocumentValidationError>> pair = errorsAndWarnings.get(key);
        List<DocumentValidationError> list = pair.getRight();
        if (list.size() < combineThreshold) {
            list.add(record);
            return;
        }

        // we went over the limit, let's just count skipped values
        pair.setLeft(pair.getLeft() + 1);
    }

    private ContainerMessage flush(ContainerMessage cm, Map<String, MutablePair<Integer, List<DocumentValidationError>>> errorsAndWarnings) {
        for (MutablePair<Integer, List<DocumentValidationError>> pair : errorsAndWarnings.values()) {
            List<DocumentValidationError> list = pair.getRight();
            for (int i = 0; i < list.size(); i++) {
                DocumentValidationError record = list.get(i);

                // last record in the list
                if (i == list.size() - 1) {
                    if (pair.getLeft() != 0) {
                        record.withLocation(record.getLocation() + " (" + pair.getLeft() + " SKIPPED)");
                    }
                }

                if ("fatal".equals(record.getFlag())) {
                    cm.getHistory().addValidationError(record);
                } else {
                    cm.getHistory().addValidationWarning(record);
                }
            }
        }

        return cm;
    }

    private DocumentValidationError parseAttributes(Attributes attr, ValidationRule rule) {
        String flag = getValue(attr, "flag");
        if ("fatal".equals(flag) || "warning".equals(flag)) {
            if (rule.getSuppress() == null || !rule.getSuppress().contains(getValue(attr, "id"))) {
                return new DocumentValidationError("Validation error")
                        .withTest(getValue(attr, "test"))
                        .withIdentifier(getValue(attr, "id"))
                        .withLocation(getValue(attr, "location"))
                        .withFlag(flag);
            }
        }
        return null;
    }

    private String getValue(Attributes attr, String id) {
        int index = attr.getIndex(id);
        if (index == -1) {
            return "N/A";
        }
        return attr.getValue(index);
    }

}
