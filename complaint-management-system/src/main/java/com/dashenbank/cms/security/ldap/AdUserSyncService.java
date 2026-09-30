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
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Autowired
    public AdUserSyncService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, Clock.systemDefaultZone());
    }

    AdUserSyncService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Login path: Identity verified via AD. If user does not have an approved role,
     * record is initialized as PENDING_APPROVAL with ROLE_PENDING.
     */
    @Transactional
    public User upsertForLogin(AdUserProfile profile) {
        User user = findExisting(profile);
        boolean created = user == null;
        if (created) {
            user = User.builder()
                    .username(profile.samAccountName())
                    .email(emailOrPlaceholder(profile))
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .fullName(name(profile))
                    .role(Role.ROLE_PENDING)
                    .authSource(AuthSource.AD)
                    .approved(false)
                    .approvalStatus("PENDING_APPROVAL")
                    .enabled(true)
                    .mustChangePassword(false)
                    .build();
        } else {
            user.setUsername(profile.samAccountName());
            if (StringUtils.hasText(profile.email())) {
                user.setEmail(profile.email().trim());
            }
            if (StringUtils.hasText(name(profile))) {
                user.setFullName(name(profile));
            }
            user.setAuthSource(AuthSource.AD);
            user.setMustChangePassword(false);
        }

        if (StringUtils.hasText(profile.objectGuid())) {
            user.setObjectGuid(profile.objectGuid());
        }
        if (StringUtils.hasText(profile.title())) {
            user.setAdJobTitle(profile.title().trim());
        }
        if (StringUtils.hasText(profile.department())) {
            user.setDepartment(profile.department().trim());
        }
        user.setLastLdapSyncAt(LocalDateTime.now(clock));

        User saved = userRepository.save(user);
        log.info("{} AD user {} approved={} status={} role={}", created ? "Created" : "Updated",
                saved.getUsername(), saved.isApproved(), saved.getApprovalStatus(), saved.getRole());
        return saved;
    }

    @Transactional
    public boolean syncIdentity(AdUserProfile profile) {
        User user = findExisting(profile);
        if (user != null) {
            if (StringUtils.hasText(profile.title())) {
                user.setAdJobTitle(profile.title().trim());
            }
            if (StringUtils.hasText(profile.department())) {
                user.setDepartment(profile.department().trim());
            }
            user.setLastLdapSyncAt(LocalDateTime.now(clock));
            userRepository.save(user);
        }
        return true;
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
