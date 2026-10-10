package br.com.devready.infrastructure.catalog.persistence;

import br.com.devready.application.catalog.port.TagRepository;
import br.com.devready.domain.catalog.Tag;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaTagRepository implements TagRepository {

    private final SpringDataTagRepository tags;

    public JpaTagRepository(SpringDataTagRepository tags) {
        this.tags = tags;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tag> findBySlug(String slug) {
        return tags.findBySlug(slug).map(CatalogMappers::toDomain);
    }

    @Override
    public Tag save(Tag tag) {
        TagEntity entity =
                tags.findById(tag.id())
                        .orElseGet(() -> new TagEntity(tag.id(), tag.slug(), tag.name()));
        entity.setName(tag.name());
        return CatalogMappers.toDomain(tags.save(entity));
    }
}
