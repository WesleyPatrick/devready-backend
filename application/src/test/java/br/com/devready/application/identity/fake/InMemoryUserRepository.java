package br.com.devready.application.identity.fake;

import br.com.devready.application.identity.dto.UserFilter;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.domain.identity.SocialAccount;
import br.com.devready.domain.identity.User;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class InMemoryUserRepository implements UserRepository {
    private static final Comparator<User> NEWEST_FIRST =
            Comparator.comparing(User::createdAt)
                    .reversed()
                    .thenComparing(User::id, Comparator.reverseOrder());

    private final Map<UUID, User> users = new LinkedHashMap<>();
    private int saveCount = 0;
    private int lockCalls = 0;
    private final Set<UUID> leftOutOfAdminCount = new HashSet<>();

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.values().stream().filter(u -> u.email().equals(email)).findFirst();
    }

    @Override
    public Optional<User> findBySocialAccount(SocialAccount account) {
        return users.values().stream()
                .filter(u -> u.socialAccounts().contains(account))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public User save(User user) {
        saveCount++;
        users.put(user.id(), user);
        return user;
    }

    @Override
    public void saveProfile(User user) {
        saveCount++;
        users.get(user.id()).updateProfile(user.displayName(), user.avatarUrl());
    }

    @Override
    public void saveCredentials(User user) {
        saveCount++;
        User stored = users.get(user.id());
        if (user.mustChangePassword()) {
            stored.resetPassword(user.passwordHash());
        } else {
            stored.changePassword(user.passwordHash());
        }
    }

    @Override
    public void saveAccess(User user) {
        saveCount++;
        User stored = users.get(user.id());
        if (user.active()) {
            stored.activate();
        } else {
            stored.deactivate();
        }
        if (user.isAdmin()) {
            stored.promoteToAdmin();
        } else {
            stored.demoteToCandidate();
        }
    }

    @Override
    public boolean replacePassword(UUID userId, String expectedHash, String newHash) {
        User stored = users.get(userId);
        if (!Objects.equals(stored.passwordHash(), expectedHash)) {
            return false;
        }
        saveCount++;
        stored.changePassword(newHash);
        return true;
    }

    @Override
    public void linkSocialAccount(UUID userId, SocialAccount account) {
        User stored = users.get(userId);
        if (!stored.hasSocialAccount(account.provider())) {
            stored.linkSocial(account);
        }
    }

    @Override
    public int count(UserFilter filter) {
        return (int) matching(filter).count();
    }

    @Override
    public List<User> find(UserFilter filter, int page, int size) {
        return matching(filter).skip((long) (page - 1) * size).limit(size).toList();
    }

    @Override
    public int lockActiveAdmins() {
        lockCalls++;
        return (int)
                users.values().stream()
                        .filter(User::isActiveAdmin)
                        .filter(u -> !leftOutOfAdminCount.contains(u.id()))
                        .count();
    }

    public void leaveOutOfAdminCount(UUID id) {
        leftOutOfAdminCount.add(id);
    }

    private Stream<User> matching(UserFilter filter) {
        return users.values().stream()
                .filter(u -> filter.role() == null || u.role() == filter.role())
                .filter(u -> filter.active() == null || u.active() == filter.active())
                .filter(
                        u ->
                                filter.text() == null
                                        || contains(u.displayName(), filter.text())
                                        || contains(u.email(), filter.text()))
                .sorted(NEWEST_FIRST);
    }

    private static boolean contains(String value, String text) {
        return value.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT));
    }

    public int count() {
        return users.size();
    }

    public List<User> all() {
        return List.copyOf(users.values());
    }

    public int saveCount() {
        return saveCount;
    }

    public void resetSaveCount() {
        saveCount = 0;
    }

    public int lockCalls() {
        return lockCalls;
    }
}
