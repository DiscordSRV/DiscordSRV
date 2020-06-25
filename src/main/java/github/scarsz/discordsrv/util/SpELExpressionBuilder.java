package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class SpELExpressionBuilder {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final String expression;
    private final EvaluationContext context = new StandardEvaluationContext();

    public SpELExpressionBuilder(String expression) {
        this.expression = expression;
    }

    public SpELExpressionBuilder withVariable(String key, Object value) {
        context.setVariable(key, value);
        return this;
    }

    public SpELExpressionBuilder withVariables(Object[] objects) {
        if (objects.length % 2 != 0) throw new IllegalArgumentException("Non-even number of objects supplied");
        for (int i = 0; i < objects.length; i += 2) context.setVariable(objects[i].toString(), objects[i + 1]);
        return this;
    }

    public SpELExpressionBuilder withPluginVariables() {
        context.setVariable("plugins", Bukkit.getPluginManager().getPlugins());
        return this;
    }

    public <T> T evaluate(Object root) {
        return (T) evaluate(root, Object.class);
    }

    public <T> T evaluate(Object root, Class<T> desiredType) {
        try {
            Expression expression = PARSER.parseExpression(this.expression);
            return expression.getValue(context, root, desiredType);
        } catch (ParseException e) {
            DiscordSRV.error("Error while parsing expression \"" + this.expression + "\" -> " + e.getMessage());
            return null;
        }
    }

}
