package br.com.devready.application.shared;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

public final class TitleOrder {

    private static final Collator COLLATOR = Collator.getInstance(Locale.forLanguageTag("pt-BR"));

    static {
        COLLATOR.setStrength(Collator.SECONDARY);
    }

    public static final Comparator<String> BY_TITLE = COLLATOR::compare;

    private TitleOrder() {}
}
