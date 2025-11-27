package org.example.javamusicapp.config;

import org.example.javamusicapp.config.auth.JwtAuthEntryPoint;
import org.example.javamusicapp.config.auth.JwtAuthenticationFilter;
import org.example.javamusicapp.config.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Wichtig für OPTIONS/CORS
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 🏰 **Was geht hier ab?**
 * Das ist die Festungsmauer unserer App. Hier wird die komplette Security geregelt. Die Klasse ist quasi
 * die Security-Zentrale und eine der wichtigsten Klassen im ganzen Projekt. No cap.
 *
 * Was sie genau macht:
 * 1.  **Routen schützen:** Legt fest, welche API-Routen public sind (z.B. `/api/auth/**` für Login/Register)
 *     und welche einen gültigen JWT-Token brauchen (basically alle anderen).
 * 2.  **Stateless-Modus:** Stellt die Sessions auf `STATELESS`. Heißt, der Server speichert keine Login-Infos.
 *     Jeder Request muss den JWT mitschicken, um sich auszuweisen. Das ist modern und skaliert besser.
 * 3.  **Filter einbauen:** Hängt unsere custom Filter in die Security-Kette rein.
 *     - `RateLimitFilter`: Kommt fast zuerst, um Spammer früh zu blocken.
 *     - `JwtAuthenticationFilter`: Kommt vor dem Standard-Login-Filter, um die JWTs zu checken.
 * 4.  **CORS:** Konfiguriert die CORS-Regeln, damit das Frontend mit dem Backend quatschen kann, ohne dass
 *     der Browser es blockt.
 * 5.  **Error Handling:** Sagt der App, dass sie den `JwtAuthEntryPoint` nutzen soll, wenn ein Non-Admin-User
 *     auf 'nen Admin-Endpoint will.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Pfade, die ohne Login zugänglich sein müssen (Auth, Swagger, CORS)
    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/lyrics/sample",
            "/api/lyrics/youtube"
    };

    private final JwtAuthEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final UserDetailsService userDetailsService;

    // Spring injiziert alle Komponenten, die es als @Component/@Service/@Bean
    // findet.
    public SecurityConfig(
            JwtAuthEntryPoint unauthorizedHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitFilter rateLimitFilter,
            UserDetailsService userDetailsService) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.userDetailsService = userDetailsService;
    }

    // -- 1. SECURITY FILTER KETTE (Hauptregelwerk) --
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> req
                        // Public endpoints (no authentication required)
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated())
                .exceptionHandling(exc -> exc.authenticationEntryPoint(unauthorizedHandler))
                // Add JWT filter before Spring Security's default authentication filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                // Add rate-limit filter before JWT filter so rate limiting applies early
                .addFilterBefore(
                        rateLimitFilter,
                        JwtAuthenticationFilter.class);

        return http.build();
    }

    // -- 2. KERN-BEANS FÜR AUTHENTIFIZIERUNG --

    // Der Provider kombiniert den User-Service (PostgreSQL) und den Encoder
    // (BCrypt)
    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    // Der AuthenticationManager verwaltet den Login-Prozess
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // Spring bindet den @Bean AuthenticationProvider automatisch in den Manager ein
        return config.getAuthenticationManager();
    }

    // -- 3. CORS KONFIGURATION (Um den hartnäckigen 401 zu beheben) --
    // Dadurch wird eine globale CORS-Regel für alle Endpunkte erstellt.
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*") // FÜR DEN TEST: Erlaube alle Ursprünge
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}