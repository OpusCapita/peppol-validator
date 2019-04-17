package com.opuscapita.peppol.validator.controller.document;

import com.opuscapita.peppol.commons.container.state.log.DocumentValidationError;
import com.opuscapita.peppol.validator.controller.validators.AttachmentValidator;
import com.opuscapita.peppol.validator.rule.ValidationRule;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;

/**
 * Reads document from storage and splits it into two parts - SBDH and document body.
 */
@Component
public class DocumentSplitter {

    private final static String MINIMAL_PDF =
            "JVBERi0xLjEKJcKlwrHDqwoKMSAwIG9iagogIDw8IC9UeXBlIC9DYXRhbG9nCiAgICAgL1BhZ2Vz\n" +
                    "IDIgMCBSCiAgPj4KZW5kb2JqCgoyIDAgb2JqCiAgPDwgL1R5cGUgL1BhZ2VzCiAgICAgL0tpZHMg\n" +
                    "WzMgMCBSXQogICAgIC9Db3VudCAxCiAgICAgL01lZGlhQm94IFswIDAgMzAwIDE0NF0KICA+Pgpl\n" +
                    "bmRvYmoKCjMgMCBvYmoKICA8PCAgL1R5cGUgL1BhZ2UKICAgICAgL1BhcmVudCAyIDAgUgogICAg\n" +
                    "ICAvUmVzb3VyY2VzCiAgICAgICA8PCAvRm9udAogICAgICAgICAgIDw8IC9GMQogICAgICAgICAg\n" +
                    "ICAgICA8PCAvVHlwZSAvRm9udAogICAgICAgICAgICAgICAgICAvU3VidHlwZSAvVHlwZTEKICAg\n" +
                    "ICAgICAgICAgICAgICAgL0Jhc2VGb250IC9UaW1lcy1Sb21hbgogICAgICAgICAgICAgICA+Pgog\n" +
                    "ICAgICAgICAgID4+CiAgICAgICA+PgogICAgICAvQ29udGVudHMgNCAwIFIKICA+PgplbmRvYmoK\n" +
                    "CjQgMCBvYmoKICA8PCAvTGVuZ3RoIDU1ID4+CnN0cmVhbQogIEJUCiAgICAvRjEgMTggVGYKICAg\n" +
                    "IDAgMCBUZAogICAgKEhlbGxvIFdvcmxkKSBUagogIEVUCmVuZHN0cmVhbQplbmRvYmoKCnhyZWYK\n" +
                    "MCA1CjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAwMDAxOCAwMDAwMCBuIAowMDAwMDAwMDc3IDAw\n" +
                    "MDAwIG4gCjAwMDAwMDAxNzggMDAwMDAgbiAKMDAwMDAwMDQ1NyAwMDAwMCBuIAp0cmFpbGVyCiAg\n" +
                    "PDwgIC9Sb290IDEgMCBSCiAgICAgIC9TaXplIDUKICA+PgpzdGFydHhyZWYKNTY1CiUlRU9GCg==";

    private final XMLInputFactory xmlInputFactory;
    private final AttachmentValidator attachmentValidator;

    @Autowired
    public DocumentSplitter(@Lazy XMLInputFactory xmlInputFactory, AttachmentValidator attachmentValidator) {
        this.xmlInputFactory = xmlInputFactory;
        this.attachmentValidator = attachmentValidator;
    }

    public DocumentSplitterResult split(InputStream inputStream, ValidationRule rule) throws XMLStreamException, IOException {
        return split(inputStream, rule, null); // use the encoding defined in the file
    }

    public DocumentSplitterResult split(InputStream inputStream, ValidationRule rule, String encoding) throws XMLStreamException, IOException {
        FastByteArrayOutputStream sbdh = new FastByteArrayOutputStream(2048); // seems like regular SBDH is inside this limit
        FastByteArrayOutputStream body = new FastByteArrayOutputStream(8192); // seems like regular file is inside this limit

        boolean collectingSbdh = false;
        boolean collectingBody = false;
        boolean putAttachment = false;

        XMLEventReader reader = StringUtils.isBlank(encoding)
                ? xmlInputFactory.createXMLEventReader(inputStream)
                : xmlInputFactory.createXMLEventReader(inputStream, encoding);

        Writer sbdhWriter = new OutputStreamWriter(sbdh);
        Writer bodyWriter = new OutputStreamWriter(body);

        DocumentValidationError attachmentError = null;

        String name = null;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                StartElement start = event.asStartElement();
                name = start.getName().getLocalPart();

                if ("StandardBusinessDocument".equals(name)) {
                    collectingSbdh = true;
                    collectingBody = false;
                    putAttachment = false;
                }
                if (rule.getLocalName().equals(name)) {
                    collectingSbdh = false;
                    collectingBody = true;
                    putAttachment = false;
                }
                if ("Attachment".equals(name)) {
                    collectingSbdh = false;
                    collectingBody = false;
                    putAttachment = true;
                }
            }

            if (collectingSbdh) {
                event.writeAsEncodedUnicode(sbdhWriter);
            }
            if (collectingBody) {
                event.writeAsEncodedUnicode(bodyWriter);
            }
            if (putAttachment) {
                if (event.isCharacters() && !event.asCharacters().isWhiteSpace() && "EmbeddedDocumentBinaryObject".equals(name)) {
                    attachmentError = attachmentValidator.validate(event.asCharacters().getData());
                    if (attachmentError != null) {
                        attachmentError.withLocation("Line: " + event.getLocation().getLineNumber() + ", column: " + event.getLocation().getColumnNumber());
                    }
                    bodyWriter.append(MINIMAL_PDF);
                } else {
                    event.writeAsEncodedUnicode(bodyWriter);
                }
            }

            if (event.isEndElement()) {
                EndElement end = event.asEndElement();
                name = end.getName().getLocalPart();
                if (rule.getLocalName().equals(name)) {
                    collectingSbdh = true;
                    collectingBody = false;
                    putAttachment = false;
                }
                if ("Attachment".equals(name)) {
                    collectingSbdh = false;
                    collectingBody = true;
                    putAttachment = false;
                }
            }
        }

        sbdhWriter.close();
        bodyWriter.close();

        return new DocumentSplitterResult(body.toByteArrayUnsafe(), sbdh.toByteArrayUnsafe(), attachmentError);
    }

}
