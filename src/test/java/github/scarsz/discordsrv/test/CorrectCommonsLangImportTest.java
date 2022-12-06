package github.scarsz.discordsrv.test;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * <p>Makes sure that no source files are importing org.apache.commons.lang.*</p>
 * <p>Instead, the import org.apache.commons.lang3.* should be used</p>
 */
public class CorrectCommonsLangImportTest {

    private final File sourceFilesRoot = new File("src/main/java");

    @Test
    public void test() {
        for (File file : FileUtils.listFiles(sourceFilesRoot, new String[] { "java" }, true)) {
            try {
                String fileSource = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                assertFalse(
                        fileSource.contains("\nimport org.apache.commons.lang."),
                        "File " + file.getPath() + " uses illegal import for org.apache.commons.lang"
                );
            } catch (IOException e) {
                System.out.println("Failed to read file " + file.getPath() + ": " + e.getMessage());
            }
        }
    }

}
