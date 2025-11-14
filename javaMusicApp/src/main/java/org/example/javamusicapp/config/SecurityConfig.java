package org.example.javamusicapp.config;

import org.example.javamusicapp.config.auth.JwtAuthEntryPoint;
import org.example.javamusicapp.config.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Wichtig für OPTIONS/CORS
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Pfade, die ohne Login zugänglich sein müssen (Auth, Swagger, CORS)
    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final JwtAuthEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    // Spring injiziert alle Komponenten, die es als @Component/@Service/@Bean findet.
    public SecurityConfig(
            JwtAuthEntryPoint unauthorizedHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            UserDetailsService userDetailsService
    ) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    // -- 1. SECURITY FILTER KETTE (Hauptregelwerk) --
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)  throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                // CORS ist wichtig für Flutter/Web-Test (wird in WebMvcConfigurer konfiguriert)
                .cors(Customizer.withDefaults())

                // State-less, da wir JWT verwenden
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))

                // Autorisierungsregeln
                .authorizeHttpRequests(req -> req
                        // Erlaube OPTIONS Pre-Flight Requests (WICHTIG für CORS/Flutter)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Gib alle öffentlichen URLs frei (Login, Swagger, etc.)
                        .requestMatchers(PUBLIC_URLS).permitAll()

                        // Schütze alle anderen Endpunkte
                        .anyRequest()
                        .authenticated())

                // Fehlerbehandlung
                .exceptionHandling(exc -> exc.authenticationEntryPoint(unauthorizedHandler))

                // JWT Filter in die Kette einfügen (VOR dem Standard-Username/Passwort-Filter)
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // -- 2. KERN-BEANS FÜR AUTHENTIFIZIERUNG --

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Der Provider kombiniert den User-Service (PostgreSQL) und den Encoder (BCrypt)
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
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