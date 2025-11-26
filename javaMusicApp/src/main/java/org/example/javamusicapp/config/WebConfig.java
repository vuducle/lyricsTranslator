package org.example.javamusicapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web-Konfiguration für die JavaMusicApp.
 * Definiert Ressourcen-Handler für statische Inhalte wie Profilbilder.
 * Autor: Vu Minh Le
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Konfiguriere den Pfad für hochgeladene Profilbilder
        registry.addResourceHandler("/uploads/profile-images/**")
                .addResourceLocations("file:uploads/profile-images/");
    }
}
