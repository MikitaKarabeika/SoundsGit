package com.ape.friction.api.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import com.ape.friction.api.common.JwsService;
import com.ape.friction.api.dto.signature.Signable;
import com.ape.friction.api.exceptions.signature.InvalidSignatureException;
import com.ape.friction.api.exceptions.signature.MissingSignatureException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@Component
@RequiredArgsConstructor
public class SignatureInterceptor {

    private final JwsService jwsService;
    private final HttpServletRequest request;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = DefaultParameterNameDiscoverer.getSharedInstance();

    @Before("Pointcuts.requireSignatureMethods()")
    public void verify(JoinPoint joinPoint) {
        String signature = request.getHeader("X-Resource-Signature");
        if (signature == null || signature.isEmpty()) {
            throw new MissingSignatureException();
        }

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        RequireSignature ann = methodSignature.getMethod().getAnnotation(RequireSignature.class);
        if (ann == null) return;

        EvaluationContext ctx = new MethodBasedEvaluationContext(
                null, methodSignature.getMethod(),
                joinPoint.getArgs(), parameterNameDiscoverer
        );

        Signable signable = getSpelValue(ann.signable(), ctx, Signable.class);

        Map<String, Object> toVerify = new HashMap<>(signable.getSignableProperties());

        if (!ann.id().isEmpty()) {
            UUID id = getSpelValue(ann.id(), ctx, UUID.class);
            toVerify.put("id", id.toString());
        }

        if (!toVerify.containsKey("id")) {
            throw new InvalidSignatureException();
        }

        jwsService.verifyMap(signature, toVerify);
    }

    private <T> T getSpelValue(String expression, EvaluationContext context, Class<T> targetClass) {
        return parser.parseExpression(expression).getValue(context, targetClass);
    }
}
