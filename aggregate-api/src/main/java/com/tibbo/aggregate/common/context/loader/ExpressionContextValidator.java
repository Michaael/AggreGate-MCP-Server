package com.tibbo.aggregate.common.context.loader;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.Context;
import com.tibbo.aggregate.common.expression.Evaluator;
import com.tibbo.aggregate.common.expression.Expression;
import com.tibbo.aggregate.common.expression.Reference;
import com.tibbo.aggregate.common.expression.ReferenceResolver;

/**
 * Validator which will accept the context if it matches the expression provided
 *
 * @author Alexander Sidorov
 * @since 01.04.2023
 * @see <a href="https://tibbotech.atlassian.net/browse/AGG-14058">AGG-14058</a>
 */
@Immutable
public class ExpressionContextValidator implements ContextValidator {

    private final Evaluator evaluator;
    private final Expression validityExpression;
    private final Reference reference;

    public ExpressionContextValidator(@Nonnull Evaluator evaluator, @Nonnull Expression validityExpression, @Nonnull Reference reference) {
        this.evaluator = evaluator;
        this.validityExpression = validityExpression;
        this.reference = reference;
    }

    @Override
    public boolean validate(String contextPath)
    {
        if (validityExpression.getText().isEmpty())
        {
            return true;
        }
        ReferenceResolver defaultResolver = evaluator.getDefaultResolver();

        CallerController caller = defaultResolver.getCallerController();

        Context context = defaultResolver.getContextManager().get(contextPath, caller);

        if (context == null)
        {
            return false;
        }

        defaultResolver.setDefaultContext(context);

        try
        {
            return evaluator.evaluateToBoolean(validityExpression, context, reference);
        }
        catch (Exception ex)
        {
            Log.CONTEXT_CHILDREN.debug("Unable to evaluate children filter expression fo the context: " + contextPath, ex);
            return false;
        }
    }
}
