package br.com.devready.domain.identity;

public enum SocialProvider {
    GOOGLE("Google"),
    GITHUB("GitHub");

    private final String label;

    SocialProvider(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
