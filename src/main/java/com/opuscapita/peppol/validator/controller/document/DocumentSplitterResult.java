package com.opuscapita.peppol.validator.controller.document;

import com.opuscapita.peppol.commons.container.state.log.DocumentValidationError;

public class DocumentSplitterResult {

    private final byte[] body;
    private final byte[] header;
    private final DocumentValidationError attachmentError;

    DocumentSplitterResult(byte[] body, byte[] header, DocumentValidationError attachmentError) {
        this.body = body;
        this.header = header;
        this.attachmentError = attachmentError;
    }

    public byte[] getBody() {
        return body;
    }

    public byte[] getHeader() {
        return header;
    }

    public DocumentValidationError getAttachmentError() {
        return attachmentError;
    }

}
