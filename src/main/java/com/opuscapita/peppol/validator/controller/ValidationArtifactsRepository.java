package com.opuscapita.peppol.validator.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;

@Component
public class ValidationArtifactsRepository {

    public final static String RULES_ROOT = "/rules/";

    private final SchemaFactory schemaFactory;
    private final TransformerFactory transformerFactory;

    @Autowired
    public ValidationArtifactsRepository(@Lazy SchemaFactory schemaFactory, @Lazy TransformerFactory transformerFactory) {
        this.schemaFactory = schemaFactory;
        this.transformerFactory = transformerFactory;
    }

    public File getArtifactAsFile(String fileName) {
        return new File(getClass().getResource(RULES_ROOT + fileName).getFile());
    }

    @Cacheable("xsd")
    public Schema getArtifactAsSchema(String fileName) throws SAXException {
        File sourceFile = getArtifactAsFile(fileName);
        return schemaFactory.newSchema(sourceFile);
    }

    @Cacheable("xsl")
    public Templates getArtifactAsTemplate(String fileName) throws TransformerConfigurationException {
        File sourceFile = getArtifactAsFile(fileName);
        Source xsltSource = new StreamSource(sourceFile);
        return transformerFactory.newTemplates(xsltSource);
    }

}
