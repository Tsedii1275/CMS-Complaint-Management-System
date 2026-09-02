package com.dashenbank.cms.security;

import com.dashenbank.cms.model.User;
import com.dashenbank.cms.repository.UserRepository;
import com.dashenbank.cms.service.AccountLockoutService;
import com.dashenbank.cms.service.PasswordPolicyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final AccountLockoutService accountLockoutService;
    private final PasswordPolicyService passwordPolicyService;

    public UserDetailsServiceImpl(UserRepository userRepository,
            AccountLockoutService accountLockoutService,
            PasswordPolicyService passwordPolicyService) {
        this.userRepository = userRepository;
        this.accountLockoutService = accountLockoutService;
        this.passwordPolicyService = passwordPolicyService;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        accountLockoutService.unlockIfElapsed(user, currentIp());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true,
                !passwordPolicyService.isExpired(user),
                !accountLockoutService.isLocked(user),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())));
    }

    private static String currentIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            return ClientIp.from(request);
        }
        return null;
    }
}
