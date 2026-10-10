package br.com.devready.application.career.port;

import br.com.devready.domain.career.Stack;
import java.util.List;
import java.util.Optional;

public interface StackRepository {
    List<Stack> findAll();

    Optional<Stack> findBySlug(String slug);

    Stack save(Stack stack);
}
