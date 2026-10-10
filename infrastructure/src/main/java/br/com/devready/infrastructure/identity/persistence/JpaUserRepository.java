package br.com.devready.infrastructure.identity.persistence;

import br.com.devready.application.identity.dto.UserFilter;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.domain.identity.Role;
import br.com.devready.domain.identity.SocialAccount;
import br.com.devready.domain.identity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository users;

    public JpaUserRepository(SpringDataUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return users.findById(id).map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return users.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findBySocialAccount(SocialAccount account) {
        return users.findBySocialAccount(account.provider(), account.providerUserId())
                .map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return users.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserEntity entity = users.findById(user.id()).orElseGet(() -> new UserEntity(user.id()));
        UserEntity saved = users.save(UserMapper.apply(user, entity));
        return UserMapper.toDomain(saved);
    }

    @Override
    public void saveProfile(User user) {
        users.updateProfile(user.id(), user.displayName(), user.avatarUrl());
    }

    @Override
    public void saveCredentials(User user) {
        users.updateCredentials(user.id(), user.passwordHash(), user.mustChangePassword());
    }

    @Override
    public boolean replacePassword(UUID userId, String expectedHash, String newHash) {
        int changed =
                expectedHash == null
                        ? users.setFirstPassword(userId, newHash)
                        : users.replacePassword(userId, expectedHash, newHash);
        return changed == 1;
    }

    @Override
    public void linkSocialAccount(UUID userId, SocialAccount account) {
        users.insertSocialAccount(
                UUID.randomUUID(), userId, account.provider().name(), account.providerUserId());
    }

    @Override
    public void saveAccess(User user) {
        users.updateAccess(user.id(), user.role(), user.active());
    }

    @Override
    @Transactional(readOnly = true)
    public int count(UserFilter filter) {
        return Math.toIntExact(
                users.countMatching(rolesOf(filter), activesOf(filter), textOf(filter)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> find(UserFilter filter, int page, int size) {
        return users
                .findMatching(
                        rolesOf(filter),
                        activesOf(filter),
                        textOf(filter),
                        PageRequest.of(page - 1, size))
                .stream()
                .map(UserMapper::toDomain)
                .toList();
    }

    @Override
    public int lockActiveAdmins() {
        return users.lockByRoleAndActive(Role.ADMIN, true).size();
    }

    private static List<Role> rolesOf(UserFilter filter) {
        return filter.role() == null ? List.of(Role.values()) : List.of(filter.role());
    }

    private static List<Boolean> activesOf(UserFilter filter) {
        return filter.active() == null ? List.of(true, false) : List.of(filter.active());
    }

    private static String textOf(UserFilter filter) {
        return filter.text() == null ? "" : filter.text();
    }
}
