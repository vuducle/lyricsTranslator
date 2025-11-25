package org.example.javamusicapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    // Simple local fixed-window map: ip -> Window(count, windowId)
    private final Map<String, Window> local = new ConcurrentHashMap<>();

    // Configuration (simple defaults)
    private final long windowSeconds = 60; // seconds per window
    private final long limit = 100; // max requests per window

    @Autowired
    public RateLimitFilter(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip rate limiting for auth, Swagger UI, API docs, and static resources
        return path.startsWith("/api/auth/") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/webjars/") ||
                path.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = extractClientIp(request);
        long nowWindow = (System.currentTimeMillis() / 1000) / windowSeconds;

        if (redisTemplate != null) {
            // Simple distributed fixed-window counter in Redis
            String key = String.format("rate:%s:%d", ip, nowWindow);
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            if (count != null && count > limit) {
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(windowSeconds));
                response.getWriter().write("Too Many Requests");
                return;
            }
            filterChain.doFilter(request, response);
        } else {
            // Local simple fixed-window counter
            Window w = local.compute(ip, (k, old) -> {
                if (old == null || old.windowId != nowWindow) {
                    return new Window(nowWindow, new AtomicLong(1));
                } else {
                    old.counter.incrementAndGet();
                    return old;
                }
            });

            if (w != null && w.counter.get() > limit) {
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(windowSeconds));
                response.getWriter().write("Too Many Requests");
                return;
            }
            filterChain.doFilter(request, response);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class Window {
        final long windowId;
        final AtomicLong counter;

        Window(long windowId, AtomicLong counter) {
            this.windowId = windowId;
            this.counter = counter;
        }
    }
}
