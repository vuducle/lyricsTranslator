package org.example.javamusicapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 🌐 **Was geht hier ab?**
 * Diese Klasse regelt allgemeine Web-Einstellungen. In diesem Fall sorgt sie dafür, dass statische
 * Ressourcen, wie z.B. hochgeladene Profilbilder, über eine URL abrufbar sind.
 *
 * Sie mappt den URL-Pfad `/uploads/profile-images/**` auf den lokalen Ordner `uploads/profile-images/`.
 * Heißt: Wenn du `http://localhost:8080/uploads/profile-images/bild.jpg` aufrufst, liefert der Server
 * die entsprechende Bild-Datei aus dem Ordner aus. Das ist wichtig, damit das Frontend die Bilder
 * auch anzeigen kann.
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
