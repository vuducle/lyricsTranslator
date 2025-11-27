package org.example.javamusicapp.config.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.javamusicapp.service.auth.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 🛂 **Was geht hier ab?**
 * Dieser Filter ist bei jeder API-Anfrage am Start (außer bei den public-Routen wie Login).
 * Er ist wie der Pass-Kontrolleur am Flughafen für fast jeden Request.
 *
 * Seine Mission:
 * 1.  Er fischt sich den `Authorization`-Header aus der Anfrage.
 * 2.  Er checkt, ob da ein "Bearer <token>" drinsteht.
 * 3.  Wenn ja, schnappt er sich den JWT (JSON Web Token) und übergibt ihn an den `JwtUtil`.
 * 4.  Der `JwtUtil` checkt, ob der Token valid (echt und nicht abgelaufen) ist.
 * 5.  Wenn alles passt, holt der Filter die User-Infos aus dem Token (z.B. Username/Email) und
 *     lädt den passenden User aus der Datenbank.
 * 6.  Am Ende sagt er Spring Security: "Yo, der User ist legit für diesen Request, lass ihn rein."
 *     Damit ist der User für diese eine Anfrage authentifiziert.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean skip = path.startsWith("/api/auth/");
        log.info("shouldNotFilter for path: {} = {}", path, skip);
        return skip;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer")) {
            filterChain.doFilter(request, response);
            return; // Beendet die Funktion hier, bevor NullPointerException auftritt
        }

        jwt = authorizationHeader.substring(7);
        final String email = jwtUtil.extractSubject(jwt);
        // 3. Wichtigste Logik (kommt als Nächstes):
        // TODO: Extrahiere den Username aus dem JWT.
        // TODO: Prüfe, ob der Token gültig ist.
        // TODO: Lade den User aus PostgreSQL via UserDetailsService.
        // TODO: Speichere den User im Spring Security Context.

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Tải UserDetails từ cơ sở dữ liệu PostgreSQL (thông qua UserService)
            UserDetails userDetails = userService.loadUserByUsername(email);
            if (jwtUtil.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // filterChain.doFilter(request, response);
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }
}
