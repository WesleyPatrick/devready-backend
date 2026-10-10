package br.com.devready.application.shared;

import java.util.function.Supplier;

public class DirectTransactions implements Transactions {
    private int calls;

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        calls++;
        return work.get();
    }

    public int calls() {
        return calls;
    }
}
