package br.com.devready.domain.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public final class SessionAssembly {

    public static final int LESSON_MIN = 3;
    public static final int LESSON_MAX = 8;

    private SessionAssembly() {}

    public static List<UUID> draw(Collection<UUID> pool, int count, RandomSource random) {
        List<UUID> shuffled = shuffled(pool, random);
        if (count < 0 || count > shuffled.size()) {
            throw new SessionException(
                    "Sorteio de " + count + " itens num banco com " + shuffled.size() + ".");
        }
        return List.copyOf(shuffled.subList(0, count));
    }

    public static List<UUID> lesson(Collection<UUID> pool, Set<UUID> solved, RandomSource random) {
        List<UUID> all = new ArrayList<>();
        all.addAll(shuffled(pool.stream().filter(id -> !solved.contains(id)).toList(), random));
        all.addAll(shuffled(pool.stream().filter(solved::contains).toList(), random));
        if (all.size() < LESSON_MIN) {
            return List.of();
        }
        return List.copyOf(all.subList(0, Math.min(LESSON_MAX, all.size())));
    }

    private static List<UUID> shuffled(Collection<UUID> ids, RandomSource random) {
        List<UUID> list = new ArrayList<>(new TreeSet<>(ids));
        for (int i = list.size() - 1; i > 0; i--) {
            Collections.swap(list, i, random.nextInt(i + 1));
        }
        return list;
    }
}
