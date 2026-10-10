package br.com.devready.application.career.fake;

import br.com.devready.application.career.port.StackRepository;
import br.com.devready.domain.career.Stack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryStackRepository implements StackRepository {
    private final Map<String, Stack> bySlug = new LinkedHashMap<>();

    @Override
    public List<Stack> findAll() {
        return new ArrayList<>(bySlug.values());
    }

    @Override
    public Optional<Stack> findBySlug(String slug) {
        return Optional.ofNullable(bySlug.get(slug));
    }

    @Override
    public Stack save(Stack stack) {
        bySlug.put(stack.slug(), stack);
        return stack;
    }

    public void remove(String slug) {
        bySlug.remove(slug);
    }

    public int count() {
        return bySlug.size();
    }
}
