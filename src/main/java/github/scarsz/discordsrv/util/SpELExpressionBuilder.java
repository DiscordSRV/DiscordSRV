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

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SpELExpressionBuilder {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final String expression;
    private final Map<String, Object> variables = new HashMap<>();

    public SpELExpressionBuilder(String expression) {
        this.expression = expression;
    }

    public SpELExpressionBuilder withVariable(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    public SpELExpressionBuilder withVariables(Map<String, Object> variables) {
        this.variables.putAll(variables);
        return this;
    }

    public SpELExpressionBuilder withPluginVariables() {
        variables.put("plugins", Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .collect(Collectors.toMap(Plugin::getName, plugin -> plugin))
        );
        return this;
    }

    public <T> T evaluate(Object root) {
        return (T) evaluate(root, Object.class);
    }

    public <T> T evaluate(Object root, Class<T> desiredType) throws ParseException, SpelEvaluationException {
        StandardEvaluationContext context = new StandardEvaluationContext(root);
        context.setVariables(variables);
        return PARSER.parseExpression(this.expression).getValue(context, desiredType);
    }

}
