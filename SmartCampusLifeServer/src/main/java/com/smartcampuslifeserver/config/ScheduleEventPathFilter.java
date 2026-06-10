package com.smartcampuslifeserver.config;

import tools.jackson.databind.ObjectMapper;
import com.smartcampuslifeserver.common.result.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 在路由匹配前统一处理 eventId 缺失/为空的路径，避免落入 401/405/500。
 */
@Component
public class ScheduleEventPathFilter extends OncePerRequestFilter {

    private static final Pattern DELETE_WITHOUT_ID = Pattern.compile("^/schedule/events/?$");
    private static final Pattern PATCH_STATUS_WITHOUT_ID = Pattern.compile("^/schedule/events/status/?$");
    private static final Pattern DELETE_WITH_ID = Pattern.compile("^/schedule/events/([^/]*)/?$");
    private static final Pattern PATCH_STATUS_WITH_ID = Pattern.compile("^/schedule/events/([^/]*)/status/?$");

    private final ObjectMapper objectMapper;

    public ScheduleEventPathFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rawPath = extractPathWithinApplication(request);
        String method = request.getMethod();

        if ("DELETE".equals(method) && isMissingEventIdOnDelete(rawPath)) {
            writeNotFound(response);
            return;
        }
        if ("PATCH".equals(method) && isMissingEventIdOnStatusUpdate(rawPath)) {
            writeNotFound(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isMissingEventIdOnDelete(String rawPath) {
        if (rawPath.contains("/schedule/events//")) {
            return true;
        }
        String path = normalizePath(rawPath);
        if (DELETE_WITHOUT_ID.matcher(path).matches()) {
            return true;
        }
        Matcher matcher = DELETE_WITH_ID.matcher(path);
        return matcher.matches() && matcher.group(1).isBlank();
    }

    private boolean isMissingEventIdOnStatusUpdate(String rawPath) {
        if (rawPath.contains("/schedule/events//")) {
            return true;
        }
        String path = normalizePath(rawPath);
        if (PATCH_STATUS_WITHOUT_ID.matcher(path).matches()) {
            return true;
        }
        Matcher matcher = PATCH_STATUS_WITH_ID.matcher(path);
        return matcher.matches() && matcher.group(1).isBlank();
    }

    private String extractPathWithinApplication(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isBlank()) {
            return servletPath;
        }
        String uri = request.getRequestURI();
        if (uri == null) {
            return "";
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String normalized = path.replaceAll("/+", "/");
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private void writeNotFound(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.error(404, "日程不存在"));
    }
}
