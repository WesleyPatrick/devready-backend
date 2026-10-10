package br.com.devready.infrastructure.career.persistence;

import br.com.devready.application.career.port.StackRepository;
import br.com.devready.domain.career.Stack;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaStackRepository implements StackRepository {

    private final SpringDataStackRepository stacks;

    public JpaStackRepository(SpringDataStackRepository stacks) {
        this.stacks = stacks;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Stack> findAll() {
        return stacks.findAll().stream().map(CareerMappers::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Stack> findBySlug(String slug) {
        return stacks.findBySlug(slug).map(CareerMappers::toDomain);
    }

    @Override
    public Stack save(Stack stack) {
        StackEntity entity =
                stacks.findById(stack.id()).orElseGet(() -> new StackEntity(stack.id()));
        return CareerMappers.toDomain(stacks.saveAndFlush(CareerMappers.apply(stack, entity)));
    }
}
