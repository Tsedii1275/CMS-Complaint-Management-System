package com.dashenbank.cms.security.ldap;

import com.dashenbank.cms.model.AuthSource;
import com.dashenbank.cms.model.Role;
import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AdUserSyncService {

    private static final Logger log = LoggerFactory.getLogger(AdUserSyncService.class);

    private final UserRepository userRepository;
    private final AdRoleMappingService roleMappingService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Autowired
    public AdUserSyncService(UserRepository userRepository, AdRoleMappingService roleMappingService,
            PasswordEncoder passwordEncoder) {
        this(userRepository, roleMappingService, passwordEncoder, Clock.systemDefaultZone());
    }

    AdUserSyncService(UserRepository userRepository, AdRoleMappingService roleMappingService,
            PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.roleMappingService = roleMappingService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Login path: role must resolve or {@link RoleNotMappedException}.
     */
    @Transactional
    public User upsertForLogin(AdUserProfile profile) {
        RoleResolution resolution = roleMappingService.resolve(profile);
        if (!resolution.resolved()) {
            throw new RoleNotMappedException(
                    "No CMS role is mapped for this Active Directory job title or group. Ask an administrator to review the mapping.");
        }
        return upsert(profile, resolution.role(), true);
    }

    /**
     * Scheduled path: existing users keep their CMS role if AD mapping is missing.
     * New users are created only when a role resolves. Org fields are never overwritten.
     */
    @Transactional
    public boolean syncIdentity(AdUserProfile profile) {
        RoleResolution resolution = roleMappingService.resolve(profile);
        User existing = findExisting(profile);
        if (existing == null) {
            if (!resolution.resolved()) {
                return false;
            }
            upsert(profile, resolution.role(), true);
            return true;
        }
        Role role = resolution.resolved() ? resolution.role() : existing.getRole();
        upsert(profile, role, resolution.resolved());
        return true;
    }

    private User upsert(AdUserProfile profile, Role role, boolean applyRole) {
        User user = findExisting(profile);
        boolean created = user == null;
        if (created) {
            user = User.builder()
                    .username(profile.samAccountName())
                    .email(emailOrPlaceholder(profile))
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .fullName(name(profile))
                    .role(role)
                    .authSource(AuthSource.AD)
                    .enabled(profile.enabled())
                    .mustChangePassword(false)
                    .build();
        } else {
            user.setUsername(profile.samAccountName());
            if (StringUtils.hasText(profile.email())) {
                user.setEmail(profile.email().trim());
            }
            user.setFullName(name(profile));
            user.setEnabled(profile.enabled());
            user.setAuthSource(AuthSource.AD);
            user.setMustChangePassword(false);
            if (applyRole) {
                user.setRole(role);
            }
        }
        if (StringUtils.hasText(profile.objectGuid())) {
            user.setObjectGuid(profile.objectGuid());
        }
        user.setAdJobTitle(profile.title());
        user.setLastLdapSyncAt(LocalDateTime.now(clock));
        User saved = userRepository.save(user);
        log.info("{} AD user {} role={} orgPreserved=true", created ? "Created" : "Updated",
                saved.getUsername(), saved.getRole());
        return saved;
    }

    private User findExisting(AdUserProfile profile) {
        if (StringUtils.hasText(profile.objectGuid())) {
            User byGuid = userRepository.findByObjectGuid(profile.objectGuid()).orElse(null);
            if (byGuid != null) {
                return byGuid;
            }
        }
        if (StringUtils.hasText(profile.samAccountName())) {
            return userRepository.findByUsernameIgnoreCase(profile.samAccountName()).orElse(null);
        }
        return null;
    }

    private static String name(AdUserProfile profile) {
        if (StringUtils.hasText(profile.displayName())) {
            return profile.displayName().trim();
        }
        return profile.samAccountName();
    }

    private static String emailOrPlaceholder(AdUserProfile profile) {
        if (StringUtils.hasText(profile.email())) {
            return profile.email().trim();
        }
        return profile.samAccountName() + "@dashenbank.com";
    }
}
