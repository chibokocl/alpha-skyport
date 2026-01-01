package com.alphaskyport.admin.security;

import com.alphaskyport.admin.model.AdminUser;
import com.alphaskyport.admin.repository.AdminUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final AdminUserRepository adminUserRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateAccessToken(jwt)) {
                UUID adminId = tokenProvider.getAdminIdFromToken(jwt);

                AdminUser admin = adminUserRepository.findByIdWithPermissions(adminId)
                        .orElse(null);

                if (admin != null && admin.getIsActive()) {
                    List<SimpleGrantedAuthority> authorities = buildAuthorities(admin);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(admin,
                            null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            log.error("Could not set admin authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private List<SimpleGrantedAuthority> buildAuthorities(AdminUser admin) {
        // Combine role-based and explicit permissions
        Stream<String> rolePermissions = admin.getRole().getDefaultPermissions().stream();
        Stream<String> explicitPermissions = admin.getPermissions().stream()
                .map(p -> p.getPermissionKey());

        return Stream.concat(rolePermissions, explicitPermissions)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/admin/auth/login")
                || path.startsWith("/api/admin/auth/refresh")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
