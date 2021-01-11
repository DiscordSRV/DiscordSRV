/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
            matcher.appendReplacement(result, key != null ? Objects.toString(replacer.apply(key)) : matcher.group(1));
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
                .evaluate(root)
        );
    }

    public static String formatExpressions(String format, Object root, Map<String, Object> variables) {
        return format(format, EXPRESSION_PATTERN, expression -> new SpELExpressionBuilder(expression)
                .withPluginVariables()
                .withVariable("server", Bukkit.getServer())
                .withVariable("discordsrv", DiscordSRV.getPlugin())
                .withVariable("jda", DiscordUtil.getJda())
                .withVariables(variables)
                .evaluate(root)
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
