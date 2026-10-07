package com.ape.friction.api.interceptors;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect @Order(Ordered.LOWEST_PRECEDENCE) //this is default, but let's be explicit. Going back from exception thrown, this logger should be applied before any handling.
@Component
public class TransactionLoggingInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TransactionLoggingInterceptor.class);

    @Before("Pointcuts.transactionLoggedAnnotatedMethods()")
    public void registerSynchronization() throws Throwable{

        // For declarative transaction management (@Transactional),
        // transaction name should refer to the component method that originated current transaction
        // We append threadId to be matched with the same id registered by method call interceptor
        final String transactionId = TransactionSynchronizationManager.getCurrentTransactionName() + ":" + String.valueOf(Thread.currentThread().threadId())
        + (null != RetrySynchronizationManager.getContext()?" (retry # "+RetrySynchronizationManager.getContext().getRetryCount()+")":"");


        TransactionSynchronizationLogger synchronizationLogger = TransactionSynchronizationLogger.threadLocalTSLogger.get();
        if (null == synchronizationLogger) {
            log.info("Creating ---------NEW--------- tslogger");
            synchronizationLogger = new TransactionSynchronizationLogger();
        } else {
            log.info("Using ---------EXISTING--------- tslogger");
        }

        synchronizationLogger.setTransactionId(transactionId); // maybe it should be a builder?
        TransactionSynchronizationManager.registerSynchronization(synchronizationLogger);
        log.trace("Transaction synchronization: {} registered", synchronizationLogger.getTransactionId());
    }

}