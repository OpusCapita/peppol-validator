package com.opuscapita.peppol.validator.consumer;

import com.opuscapita.peppol.commons.container.ContainerMessage;
import com.opuscapita.peppol.commons.container.state.Endpoint;
import com.opuscapita.peppol.commons.container.state.log.DocumentErrorType;
import com.opuscapita.peppol.commons.container.state.log.DocumentLog;
import com.opuscapita.peppol.commons.container.state.log.DocumentValidationError;
import com.opuscapita.peppol.commons.storage.Storage;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@EnableAutoConfiguration
public class ValidatorMessageConsumerTest {

    @Autowired
    private ValidatorMessageConsumer consumer;

    @Autowired
    private Storage storage;

    @Test
    @Ignore
    public void goThroughTestMaterials() throws Exception {
        File testFiles = new File(ValidatorMessageConsumerTest.class.getResource("/test-materials").getFile());
        processDirectory(testFiles);
    }

    private void processDirectory(File dir) throws Exception {
        List<String> list = Arrays.asList(dir.list());
        if (list.contains("ignore")) {
            return;
        }

        for (String fileName : list) {
            File file = new File(dir, fileName);
            if (file.isDirectory()) {
                processDirectory(file);
            } else {
                processFile(file);
            }
        }
    }

    private void processFile(File file) throws Exception {
        List<String> expected = getExpected(file);

        System.out.println("TESTING: " + file.getAbsolutePath());
        String path;
        try (InputStream stream = new FileInputStream(file)) {
            path = storage.putToCustom(stream, "/private/peppol/test/", file.getName());
        }

        ContainerMessage cm = new ContainerMessage(path, Endpoint.TEST);
        consumer.consume(cm);

        assertTrue(compare(cm, expected));
        System.out.println("PASSED: " + file.getAbsolutePath());
    }

    // an assumption is made that file is formatted
    private List<String> getExpected(File file) throws IOException {
        List<String> lines = FileUtils.readLines(file, Charset.defaultCharset());
        List<String> expected = getExpected(lines, new ArrayList<>(), "Error", "E: ");
        return getExpected(lines, expected, "Warning", "W: ");
    }

    private List<String> getExpected(List<String> lines, List<String> expected, String header, String prefix) {
        boolean reading = false;

        for (String line : lines) {
            line = line.trim();
            if (reading) {
                if (line.equals("") || "none".equalsIgnoreCase(line) || "-->".equals(line)) {
                    return expected;
                }
                if (line.contains(" x ")) {
                    // PEPPOL_CORE_R001 x 6
                    String parts[] = line.split(" x ");
                    for (int i = 0; i < Integer.parseInt(parts[1]); i++) {
                        expected.add(prefix + parts[0]);
                    }
                } else {
                    // BII2-T10-R026
                    expected.add(prefix + line);
                }
            }

            if (line.startsWith(header)) {
                reading = true;
            }
        }

        return expected;
    }

    private boolean compare(ContainerMessage cm, List<String> expected) {
        boolean passed = true;
        for (DocumentLog log : cm.getHistory().getLogs()) {
            if (!log.isInfo()) {
                if (DocumentErrorType.VALIDATION_ERROR.equals(log.getErrorType())) {
                    DocumentValidationError err = log.getValidationError();
                    String line = log.isError() ? "E: " : "W: ";
                    line += (err == null ? "null" : err.getIdentifier());

                    if (expected.contains(line)) {
                        expected.remove(line);
                    } else {
                        System.err.println("Unexpected [" + line + "] in file " + cm.getFileName());
                        System.err.println("\t" + log.getMessage());
                        passed = false;
                    }
                } else {
                    fail("File got " + log.getErrorType() + ", not validation error");
                }

            }

        }

        for (String line : expected) {
            System.err.println("Expected [" + line + "] not in file " + cm.getFileName());
            passed = false;
        }
        return passed;
    }

}

