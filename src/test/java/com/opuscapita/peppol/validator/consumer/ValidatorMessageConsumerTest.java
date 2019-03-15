package com.opuscapita.peppol.validator.consumer;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ValidatorMessageConsumerTest {

    @Test
    @Ignore
    public void goThroughTestMaterials() throws IOException {
        File testFiles = new File(getClass().getResource("/test-materials").getFile());
        processDirectory(testFiles);
    }

    @Test
    @Ignore
    public void testSingleFile() throws IOException {
        File file = new File(getClass().getResource("/test-materials/peppol-bis/invoice/T10-0041-valid-profile05.xml").getFile());
        processFile(file);
    }

    private void processDirectory(File dir) throws IOException {
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

    private void processFile(File file) throws IOException {
        List<String> expected = getExpected(file);

        System.out.println("TESTING: " + file.getAbsolutePath());

//        assertTrue(compare(cm, expected));
//        System.out.println("PASSED: " + file.getAbsolutePath() +
//                " [" + cm.getDocumentInfo().getErrors().size() + " error(s), " +
//                cm.getDocumentInfo().getWarnings().size() + " warning(s)]");
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

}
