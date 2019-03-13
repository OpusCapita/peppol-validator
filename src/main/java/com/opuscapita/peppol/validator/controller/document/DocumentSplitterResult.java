package com.opuscapita.peppol.validator.controller.document;

import com.opuscapita.peppol.commons.validation.ValidationError;

public class DocumentSplitterResult {

    private final byte[] sbdh;
    private final byte[] documentBody;
    private final ValidationError attachmentError;

    DocumentSplitterResult(byte[] sbdh, byte[] documentBody, ValidationError attachmentError) {
        this.sbdh = sbdh;
        this.documentBody = documentBody;
        this.attachmentError = attachmentError;
    }

    public byte[] getSbdh() {
        return sbdh;
    }

    public byte[] getDocumentBody() {
        return documentBody;
    }

    public ValidationError getAttachmentError() {
        return attachmentError;
    }
}
