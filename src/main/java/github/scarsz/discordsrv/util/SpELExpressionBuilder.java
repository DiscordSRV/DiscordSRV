package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
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

    public <T> T evaluate(Object root, Class<T> desiredType) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext(root);
            context.setVariables(variables);
            return PARSER.parseExpression(this.expression).getValue(context, desiredType);
        } catch (ParseException e) {
            DiscordSRV.error("Error while parsing expression \"" + this.expression + "\" -> " + e.getMessage());
        } catch (SpelEvaluationException e) {
            DiscordSRV.error("Error while evaluating expression \"" + this.expression + "\" -> " + e.getMessage());
        }
        return null;
    }

}
