package br.com.devready.application.catalog.fake;

import br.com.devready.application.catalog.port.TagRepository;
import br.com.devready.domain.catalog.Tag;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryTagRepository implements TagRepository {
    private final Map<String, Tag> tags = new HashMap<>();

    @Override
    public Optional<Tag> findBySlug(String slug) {
        return Optional.ofNullable(tags.get(slug));
    }

    @Override
    public Tag save(Tag tag) {
        tags.put(tag.slug(), tag);
        return tag;
    }

    public int count() {
        return tags.size();
    }
}
