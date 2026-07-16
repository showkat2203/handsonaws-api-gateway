package com.plm.api.graphql;

import com.plm.service.exception.AuthorizationException;
import com.plm.service.exception.NotFoundException;
import com.plm.service.exception.ValidationException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/** Maps internal-service-layer exceptions to well-formed GraphQL errors instead of leaking stack traces. */
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    private static final Logger log = LoggerFactory.getLogger(GraphQlExceptionResolver.class);

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof NotFoundException) {
            return build(ex, ErrorType.NOT_FOUND, env);
        }
        if (ex instanceof ValidationException) {
            return build(ex, ErrorType.BAD_REQUEST, env);
        }
        if (ex instanceof AuthorizationException) {
            return build(ex, ErrorType.FORBIDDEN, env);
        }
        log.error("Unhandled exception resolving field {}", env.getField().getName(), ex);
        return build(new RuntimeException("Internal server error"), ErrorType.INTERNAL_ERROR, env);
    }

    private GraphQLError build(Throwable ex, ErrorType type, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .message(ex.getMessage())
                .errorType(type)
                .build();
    }
}
