package com.ape.friction.api.interceptors;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Repository;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repository
public @interface JaversUpdateRepository {
    @AliasFor(annotation = Repository.class)
    String value() default "";
}
