package br.com.devready.application.catalog.port;

import br.com.devready.domain.catalog.Tag;
import java.util.Optional;

public interface TagRepository {
    Optional<Tag> findBySlug(String slug);

    Tag save(Tag tag);
}
