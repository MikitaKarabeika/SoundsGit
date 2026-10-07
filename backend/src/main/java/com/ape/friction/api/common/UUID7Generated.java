package com.ape.friction.api.common;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@IdGeneratorType(UUID7Generator.class)
@Target(FIELD)
@Retention(RUNTIME)
public @interface UUID7Generated {
}
