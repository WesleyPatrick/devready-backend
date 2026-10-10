package br.com.devready.application.shared;

import java.util.function.Supplier;

public interface Transactions {
    <T> T inTransaction(Supplier<T> work);
}
