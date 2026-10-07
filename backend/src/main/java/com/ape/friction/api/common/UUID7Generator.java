package com.ape.friction.api.common;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;

import java.util.EnumSet;

public class UUID7Generator implements BeforeExecutionGenerator {

    @Override
    public Object generate(
            SharedSessionContractImplementor session,
            Object owner,
            Object currentValue,
            EventType eventType
    ) {
        return UUID7.randomV7();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EnumSet.of(EventType.INSERT);
    }
}
