package com.opuscapita.peppol.validator.rule;

import com.opuscapita.peppol.commons.container.ContainerMessage;
import com.opuscapita.peppol.commons.container.state.log.DocumentLog;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SvefakturaRuleExceptions {

    private static List<String> get() {
        return Arrays.asList(
                "Invalid content was found starting with element 'sh:StandardBusinessDocument'",
                "Cannot find the declaration of element 'Invoice'",
                "Cannot find the declaration of element 'ObjectEnvelope'"
        );
    }

    public static void except(ContainerMessage cm) {
        Iterator<DocumentLog> logIterator = cm.getHistory().getLogs().iterator();
        while (logIterator.hasNext()) {
            DocumentLog error = logIterator.next();
            get().forEach(knownError -> {
                if (error.getMessage().contains(knownError)) {
                    logIterator.remove();
                }
            });
        }
    }
}
