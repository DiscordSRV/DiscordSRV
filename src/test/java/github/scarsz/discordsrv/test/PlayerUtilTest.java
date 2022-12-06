package github.scarsz.discordsrv.test;

import org.junit.jupiter.api.Test;

import static github.scarsz.discordsrv.util.PlayerUtil.convertTargetSelectors;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlayerUtilTest {

    @Test
    public void parseValidTargetSelectors() {
        assertEquals("Is it {TARGET} selector ?", convertTargetSelectors("Is it @a selector ?", null));
        assertEquals("Nested {TARGET}{TARGET}", convertTargetSelectors("Nested @e@r[limit=2]", null));
        assertEquals("End {TARGET}", convertTargetSelectors("End @r", null));
    }

    @Test
    public void parseInvalidTargetSelectors() {
        assertEquals("@e[team= @u @", convertTargetSelectors("@e[team= @u @", null));
        assertEquals("@everyone @ax", convertTargetSelectors("@everyone @ax", null));
    }

}
