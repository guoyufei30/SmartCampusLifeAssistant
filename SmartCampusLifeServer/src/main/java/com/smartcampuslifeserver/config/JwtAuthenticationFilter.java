package com.smartcampuslifeserver.config;

import tools.jackson.databind.ObjectMapper;
import com.smartcampuslifeserver.common.utils.JwtUtil;
import com.smartcampuslifeserver.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> WHITELIST = List.of(
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/auth/send_verify_code",
            "/uploads/**"
    );

    private final JwtUtil jwtUtil;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, AuthService authService, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        return WHITELIST.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractBearerToken(request.getHeader("Authorization"));

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtUtil.validateToken(token)) {
            JwtAuthenticationEntryPoint.writeJsonError(response, objectMapper, 401, "Token无效或已过期");
            return;
        }
        if (jwtUtil.isRefreshToken(token)) {
            JwtAuthenticationEntryPoint.writeJsonError(response, objectMapper, 401,
                    "不能使用 refreshToken 访问接口，请使用登录响应 data.token");
            return;
        }

        if (authService.isTokenBlacklisted(token)) {
            JwtAuthenticationEntryPoint.writeJsonError(response, objectMapper, 401, "Token已失效");
            return;
        }

        String userId = jwtUtil.getUserId(token);
        String role = jwtUtil.getRole(token);
        if (role == null) {
            role = "user";
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        String header = authorizationHeader.trim();
        if (header.isEmpty()) {
            return null;
        }
        if (header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        // 兼容 Apifox 等工具只填写纯 JWT、未加 Bearer 前缀的情况
        if (header.startsWith("eyJ")) {
            return header;
        }
        return null;
    }
}
