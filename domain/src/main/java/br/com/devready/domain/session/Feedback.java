package br.com.devready.domain.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Feedback {

    public static final String OTHERS = "Outros";

    private Feedback() {}

    public static List<CompetencyFeedback> of(List<FeedbackItem> items) {
        Map<String, int[]> tally = new LinkedHashMap<>();
        Map<String, String> names = new HashMap<>();
        int[] others = new int[2];
        for (FeedbackItem item : items) {
            int[] counts =
                    item.competencySlug() == null
                            ? others
                            : tally.computeIfAbsent(item.competencySlug(), slug -> new int[2]);
            if (item.competencySlug() != null) {
                names.putIfAbsent(item.competencySlug(), item.competencyName());
            }
            counts[1]++;
            if (item.hit()) {
                counts[0]++;
            }
        }
        List<CompetencyFeedback> result = new ArrayList<>();
        tally.forEach(
                (slug, counts) ->
                        result.add(feedback(slug, names.get(slug), counts[0], counts[1])));
        if (others[1] > 0) {
            result.add(feedback(null, OTHERS, others[0], others[1]));
        }
        return result;
    }

    private static CompetencyFeedback feedback(String slug, String name, int hits, int total) {
        FeedbackVerdict verdict = FeedbackVerdict.of(hits, total);
        return new CompetencyFeedback(
                slug, name, hits, total, verdict, text(slug == null, name, hits, total, verdict));
    }

    private static String text(
            boolean others, String name, int hits, int total, FeedbackVerdict verdict) {
        String where = others ? "outros temas" : name;
        String count = hits + " de " + total + " em " + where;
        return switch (verdict) {
            case STRONG -> "Você acertou " + count + ": ponto forte.";
            case OK -> "Você acertou " + count + ": falta pouco para virar ponto forte.";
            case GAP ->
                    count
                            + (others
                                    ? ": vale revisar esses temas."
                                    : ": vale revisar os materiais da competência.");
        };
    }
}
