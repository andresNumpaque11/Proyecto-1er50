package com.sigtam.ms_turnos.infrastructure.config;

import com.sigtam.ms_turnos.application.port.out.TransactionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class SpringTransactionAdapter implements TransactionPort {
    @Override
    public void afterCommit(Runnable action) {
        register(action, false);
    }

    @Override
    public void afterRollback(Runnable action) {
        register(action, true);
    }

    private void register(Runnable action, boolean rollbackOnly) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (!rollbackOnly) {
                            action.run();
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (rollbackOnly && status != STATUS_COMMITTED) {
                            action.run();
                        }
                    }
                });
    }
}
