package com.ape.friction.api.interceptors;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;


public class TransactionSynchronizationLogger implements TransactionSynchronization {

    public static class TransactionNotCommittedException  extends RuntimeException {}
    public static final ThreadLocal<TransactionSynchronizationLogger> threadLocalTSLogger = new ThreadLocal<>();
    private static final Logger log = LoggerFactory.getLogger(TransactionSynchronizationLogger.class);

    public static enum TransactionSynchronizationStatus {COMMITED, ROLLED_BACK, UNKNOWN};

    @Getter @Setter
    private String transactionId;

    @Getter
    private boolean committed = false;

    @Getter @Setter
    private boolean rolledBack;

    @Override
    public void beforeCommit(boolean readOnly) {
        log.info("Transaction synchronization: {} goes to commit as {}", transactionId, readOnly?"RO":"RW");
    }

    @Override
    public void afterCompletion(int status) {

        TransactionSynchronizationStatus txStatus = TransactionSynchronizationStatus.values()[status];
        log.info("Transaction synchronization: {} is after completion with: {}", transactionId, txStatus);
        committed = (txStatus == TransactionSynchronizationStatus.COMMITED);
    }
}
