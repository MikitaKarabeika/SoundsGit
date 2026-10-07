package com.ape.friction.api.interceptors;

import org.aspectj.lang.annotation.Pointcut;

public class Pointcuts {
    // This pointcut defines which methods are affected by this interceptor (all SpringData repositories)
    @Pointcut(
            "execution(public * org.springframework.data.repository.Repository+.*(..))"
    )
    public void allRepositoryMethods() {
    }

    // This pointcut defines which methods are affected by this interceptor
    // First  part: methods explicitly annotated with @MethodCallLogged
    // Second part: methods from classes annotated with @MethodCallLogged
    @Pointcut(
            "execution(@com.ape.friction.api.interceptors.MethodCallLogged * *(..)) " +
                    "|| within(@com.ape.friction.api.interceptors.MethodCallLogged *)"
    )
    public void methodCallLoggedAnnotatedMethods() {}

    // As above, but for other annotation
    @Pointcut(
            "execution(@com.ape.friction.api.interceptors.TransactionLogged * *(..)) " +
                    "|| within(@com.ape.friction.api.interceptors.TransactionLogged *)"
    )
    public void transactionLoggedAnnotatedMethods() {}


    @Pointcut(
            "execution(@com.ape.friction.api.interceptors.RequireSignature  * *(..)) " +
                    "|| within(@com.ape.friction.api.interceptors.RequireSignature  *)"
    )
    public void requireSignatureMethods() {}

    @Pointcut(
            "execution(@com.ape.friction.api.interceptors.SessionLogged  * *(..)) " +
                    "|| within(@com.ape.friction.api.interceptors.SessionLogged  *)"
    )
    public void sessionLoggedAnnotatedMethods() {}
}
