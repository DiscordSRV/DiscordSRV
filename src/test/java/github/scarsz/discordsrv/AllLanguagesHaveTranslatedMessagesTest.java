package github.scarsz.discordsrv;

import github.scarsz.discordsrv.util.LangUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p>Makes sure that all LangUtil Languages have a translation for every message</p>
 */
public class AllLanguagesHaveTranslatedMessagesTest {

    @Test
    public void test() {
        for (LangUtil.InternalMessage internalMessage : LangUtil.InternalMessage.values()) {
            for (LangUtil.Language language : LangUtil.Language.values()) {
                Assert.assertTrue(
                        "Language " + language.getName() + " missing from definitions for " + internalMessage.name(),
                        internalMessage.getDefinitions().containsKey(language)
                );
            }
        }
    }

}
