package br.com.devready.application.identity.port;

import br.com.devready.application.identity.dto.UserFilter;
import br.com.devready.domain.identity.SocialAccount;
import br.com.devready.domain.identity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findBySocialAccount(SocialAccount account);

    boolean existsByEmail(String email);

    User save(User user);

    void saveProfile(User user);

    void saveCredentials(User user);

    boolean replacePassword(UUID userId, String expectedHash, String newHash);

    void linkSocialAccount(UUID userId, SocialAccount account);

    void saveAccess(User user);

    int count(UserFilter filter);

    List<User> find(UserFilter filter, int page, int size);

    int lockActiveAdmins();
}
