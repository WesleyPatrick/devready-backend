package br.com.devready.infrastructure.identity.persistence;

import br.com.devready.domain.identity.Role;
import br.com.devready.domain.identity.SocialProvider;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query(
            "select u from UserEntity u join u.socialAccounts s "
                    + "where s.provider = :provider and s.providerUserId = :providerUserId")
    Optional<UserEntity> findBySocialAccount(
            @Param("provider") SocialProvider provider,
            @Param("providerUserId") String providerUserId);

    @Query(
            "select count(u) from UserEntity u"
                    + " where u.role in :roles and u.active in :actives"
                    + " and (locate(lower(:text), lower(u.displayName)) > 0"
                    + " or locate(lower(:text), lower(u.email)) > 0)")
    long countMatching(
            @Param("roles") Collection<Role> roles,
            @Param("actives") Collection<Boolean> actives,
            @Param("text") String text);

    @Query(
            "select u from UserEntity u"
                    + " where u.role in :roles and u.active in :actives"
                    + " and (locate(lower(:text), lower(u.displayName)) > 0"
                    + " or locate(lower(:text), lower(u.email)) > 0)"
                    + " order by u.createdAt desc, u.id desc")
    List<UserEntity> findMatching(
            @Param("roles") Collection<Role> roles,
            @Param("actives") Collection<Boolean> actives,
            @Param("text") String text,
            Pageable page);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "update UserEntity u set u.displayName = :displayName, u.avatarUrl = :avatarUrl"
                    + " where u.id = :id")
    int updateProfile(
            @Param("id") UUID id,
            @Param("displayName") String displayName,
            @Param("avatarUrl") String avatarUrl);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "update UserEntity u set u.passwordHash = :passwordHash,"
                    + " u.mustChangePassword = :mustChangePassword where u.id = :id")
    int updateCredentials(
            @Param("id") UUID id,
            @Param("passwordHash") String passwordHash,
            @Param("mustChangePassword") boolean mustChangePassword);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "update UserEntity u set u.passwordHash = :newHash, u.mustChangePassword = false"
                    + " where u.id = :id and u.passwordHash = :expectedHash")
    int replacePassword(
            @Param("id") UUID id,
            @Param("expectedHash") String expectedHash,
            @Param("newHash") String newHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "update UserEntity u set u.passwordHash = :newHash, u.mustChangePassword = false"
                    + " where u.id = :id and u.passwordHash is null")
    int setFirstPassword(@Param("id") UUID id, @Param("newHash") String newHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            nativeQuery = true,
            value =
                    "insert into social_accounts (id, user_id, provider, provider_user_id)"
                            + " values (:id, :userId, :provider, :providerUserId)")
    int insertSocialAccount(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("provider") String provider,
            @Param("providerUserId") String providerUserId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UserEntity u set u.role = :role, u.active = :active where u.id = :id")
    int updateAccess(
            @Param("id") UUID id, @Param("role") Role role, @Param("active") boolean active);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.role = :role and u.active = :active order by u.id")
    List<UserEntity> lockByRoleAndActive(@Param("role") Role role, @Param("active") boolean active);
}
