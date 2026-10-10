package br.com.devready.domain.identity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class User {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UUID id;
    private final String email;
    private String displayName;
    private String avatarUrl;
    private String passwordHash;
    private Role role;
    private boolean active;
    private boolean mustChangePassword;
    private final Instant createdAt;
    private final List<SocialAccount> socialAccounts;

    private User(
            UUID id,
            String email,
            String displayName,
            String avatarUrl,
            String passwordHash,
            Role role,
            boolean active,
            boolean mustChangePassword,
            Instant createdAt,
            List<SocialAccount> socialAccounts) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.mustChangePassword = mustChangePassword;
        this.createdAt = createdAt;
        this.socialAccounts = new ArrayList<>(socialAccounts);
    }

    public static User register(
            UUID id, String email, String displayName, String passwordHash, Instant now) {
        return new User(
                id,
                normalizeEmail(email),
                requireDisplayName(displayName),
                null,
                requireText(passwordHash, "hash da senha"),
                Role.CANDIDATE,
                true,
                false,
                now,
                List.of());
    }

    public static User fromSocial(
            UUID id,
            String email,
            String displayName,
            String avatarUrl,
            SocialAccount account,
            Instant now) {
        return new User(
                id,
                normalizeEmail(email),
                requireDisplayName(displayName),
                avatarUrl,
                null,
                Role.CANDIDATE,
                true,
                false,
                now,
                List.of(account));
    }

    public static User restore(
            UUID id,
            String email,
            String displayName,
            String avatarUrl,
            String passwordHash,
            Role role,
            boolean active,
            boolean mustChangePassword,
            Instant createdAt,
            List<SocialAccount> socialAccounts) {
        return new User(
                id,
                email,
                displayName,
                avatarUrl,
                passwordHash,
                role,
                active,
                mustChangePassword,
                createdAt,
                socialAccounts);
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            throw new InvalidEmailException("null");
        }
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalized).matches()) {
            throw new InvalidEmailException(email);
        }
        return normalized;
    }

    private static String requireDisplayName(String displayName) {
        return requireText(displayName, "nome").strip();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IdentityException("Campo obrigatório: " + field);
        }
        return value;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public boolean active() {
        return active;
    }

    public boolean mustChangePassword() {
        return mustChangePassword;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public List<SocialAccount> socialAccounts() {
        return List.copyOf(socialAccounts);
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isActiveAdmin() {
        return active && role == Role.ADMIN;
    }

    public boolean canManageUsers() {
        return isActiveAdmin() && !mustChangePassword;
    }

    public boolean hasSocialAccount(SocialProvider provider) {
        return socialAccounts.stream().anyMatch(account -> account.provider() == provider);
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = requireText(newPasswordHash, "hash da senha");
        this.mustChangePassword = false;
    }

    public void resetPassword(String temporaryPasswordHash) {
        this.passwordHash = requireText(temporaryPasswordHash, "hash da senha");
        this.mustChangePassword = true;
    }

    public void updateProfile(String newDisplayName, String newAvatarUrl) {
        this.displayName = requireDisplayName(newDisplayName);
        this.avatarUrl = newAvatarUrl;
    }

    public void linkSocial(SocialAccount account) {
        if (hasSocialAccount(account.provider())) {
            throw new IdentityException(
                    "Já existe uma conta " + account.provider().label() + " vinculada.");
        }
        socialAccounts.add(account);
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void promoteToAdmin() {
        this.role = Role.ADMIN;
    }

    public void demoteToCandidate() {
        this.role = Role.CANDIDATE;
    }
}
