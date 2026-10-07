package com.ape.friction.api.interceptors;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RequireSignature {
    String id() default "";
    String signable() default "";
}
