package org.example.javamusicapp.config.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 🚫 **Was geht hier ab?**
 * Wenn ein User ohne gültigen oder mit abgelaufenem Token versucht, auf eine geschützte Seite zuzugreifen,
 * kickt dieser EntryPoint rein.
 *
 * Statt 'ner random Fehlerseite oder einem HTML-Login schickt er eine saubere `401 Unauthorized`
 * JSON-Antwort zurück. So weiß das Frontend direkt, dass der User nicht eingeloggt oder sein
 * Token abgelaufen ist und kann ihn z.B. zur Login-Seite schicken. Sorgt für eine cleane
 * Kommunikation zwischen Front- und Backend bei Auth-Fehlern.
 */
@Slf4j
@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.error("Unauthorized access (Unauthorizierter Zugang): {}", authException.getMessage());

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        response.getWriter().write("{ \"error\": \"Unauthorized - Kein Zugang\", \"message\": \"" + authException.getMessage() + "\" }");
    }
}
