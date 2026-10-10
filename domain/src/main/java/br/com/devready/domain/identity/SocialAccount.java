package br.com.devready.domain.identity;

public record SocialAccount(SocialProvider provider, String providerUserId) {
    public SocialAccount {
        if (provider == null || providerUserId == null || providerUserId.isBlank()) {
            throw new IdentityException("Conta social precisa de provedor e identificador.");
        }
    }
}
