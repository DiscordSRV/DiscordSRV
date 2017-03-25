package github.scarsz.discordsrv;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * <p>Makes sure that no source files are importing org.apache.commons.lang.*</p>
 * <p>Instead, the import org.apache.commons.lang3.* import should be used</p>
 */
public class CorrectCommonsLangImportTest {

    private final File sourceFilesRoot = new File("src/main/java");

    @Test
    public void test() {
        for (File file : FileUtils.listFiles(sourceFilesRoot, new String[] { "java" }, true)) {
            try {
                String[] fileSource = FileUtils.readFileToString(file, Charset.defaultCharset()).split("\n");
                for (String line : fileSource) {
                    if (line.startsWith("import org.apache.commons.lang.")) {
                        Assert.fail("File " + file.getPath() + " uses illegal import " + line);
                    }
                }
            } catch (IOException e) {
                System.out.println("Failed to read file " + file.getPath() + ": " + e.getMessage());
            }
        }
    }

}
