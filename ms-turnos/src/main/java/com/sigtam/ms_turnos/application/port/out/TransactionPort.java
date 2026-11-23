package com.sigtam.ms_turnos.application.port.out;

public interface TransactionPort {
    void afterCommit(Runnable action);
    void afterRollback(Runnable action);
}
