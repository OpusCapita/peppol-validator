package com.opuscapita.peppol.validator.controller.document;

import com.opuscapita.peppol.commons.validation.ValidationError;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class AttachmentValidator {

    /**
     * Checks that attachment is a real BASE64 string.
     */
    public ValidationError validate(@NotNull String characters) {
        if (isValidBase64(characters)) {
            return null;
        }
        if (characters.length() > 100) {
            characters = characters.substring(0, 99) + "...";
        }
        if (characters.length() == 0) {
            characters = "[empty]";
        }
        return new ValidationError("Validation error")
                .withText("[ATTACHMENT] - The attachment is not base64 encoded string: " + characters)
                .withTest("Attachment contains only base64 allowed symbols and attachment length % 4 == 0")
                .withFlag("fatal")
                .withIdentifier("ATTACHMENT");
    }

    // allow all symbols in A-Za-z0-9+/ in groups of 4 padded with 1 or two '=' symbols
    boolean isValidBase64(@NotNull String characters) {
        int length = 0;
        boolean padding = false;
        int paddingLength = 0;
        for (int i = 0; i < characters.length(); i++) {
            char it = characters.charAt(i);
            // whitespace
            if (it == 9 || it == 10 || it == 13 || it == 32) {
                continue;
            }
            if (((it >= '0' && it <= '9') || (it >= 'a' && it <= 'z') || (it >= 'A' && it <= 'Z') || it == '/' || it == '+') && !padding) {
                length++;
                continue;
            }
            if (it == '=') {
                padding = true;
                paddingLength++;
                if (paddingLength > 2) {
                    return false;
                }
                length++;
                continue;
            }
            return false;
        }
        return length % 4 == 0 && length > 0;
    }

}
