package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Utility for replacing placeholders in Strings. You can provide the replacements directly as map entries or you can
 * provide a Function<String, String> that will map keys to their values.</p>
 *
 * <p>Examples:
 * <ol>
 *     <li><pre>NamedValueFormatter.format("input {string}", key -> "value") = "input value"</pre></li>
 *     <li><pre>NamedValueFormatter.format("input {string}", Map.of("string", "value")) = "input value"</pre></li>
 * </ol>
 * </p>
 */
public abstract class NamedValueFormatter {

    // https://regex101.com/r/jBeA8A
    private static final Pattern PATTERN = Pattern.compile("\\\\(.)|\\{(.+?)}");
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\\\(.)|\\$\\{(.+?)}");

    public static String format(String format, Pattern pattern, Function<String, Object> replacer) {
        Matcher matcher = pattern.matcher(format);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(2);
            matcher.appendReplacement(result, key != null ? replacer.apply(key).toString() : matcher.group(1));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Replace placeholders in the given format String with values provided by the given Function
     *
     * @param format the format to process
     * @param replacer the function that should map placeholder keys to their values
     * @return the formatted String
     */
    public static String format(String format, Function<String, Object> replacer) {
        return format(format, PATTERN, replacer);
    }

    /**
     * Replace expressions in the given format String with the evaluated results
     *
     * @param format the format to process
     * @param root the root object
     * @return the formatted String
     */
    public static String formatExpressions(String format, Object root) {
        return format(format, EXPRESSION_PATTERN, expression -> new SpELExpressionBuilder(expression)
                .withPluginVariables()
                .withVariable("server", Bukkit.getServer())
                .withVariable("discordsrv", DiscordSRV.getPlugin())
                .withVariable("jda", DiscordUtil.getJda())
                .evaluate(root).toString()
        );
    }

    public static String formatExpressions(String format, Object root, Object... objects) {
        return format(format, EXPRESSION_PATTERN, expression -> new SpELExpressionBuilder(expression)
                .withPluginVariables()
                .withVariable("server", Bukkit.getServer())
                .withVariable("discordsrv", DiscordSRV.getPlugin())
                .withVariable("jda", DiscordUtil.getJda())
                .withVariables(objects)
                .evaluate(root).toString()
        );
    }

    /**
     * Replace placeholders in the given format String with values in the given Map.
     *
     * @param format the format to process
     * @param values the keys that should be replaced in the format and their values
     * @return the formatted String
     */
    public static String format(String format, Map<String, Object> values) {
        return format(format, key -> values.getOrDefault(key, "{" + key + "}"));
    }

    public static String format(String format, Object... objects) {
        if (objects.length % 2 != 0) throw new IllegalArgumentException("Non-even number of objects supplied");
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < objects.length; i += 2) {
            map.put(objects[i].toString(), objects[i + 1]);
        }
        return format(format, map);
    }

}
