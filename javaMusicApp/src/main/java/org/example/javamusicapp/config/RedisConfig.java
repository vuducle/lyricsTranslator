package org.example.javamusicapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory; // NEUER Import
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 🚀 **Was geht hier ab?**
 * Hier wird die Connection zu Redis klargemacht. Redis ist 'ne geisteskrank schnelle In-Memory-Datenbank.
 * Wir nutzen die als Cache, um Daten, die oft gebraucht werden, zwischenzuspeichern (z.B. User-Sessions,
 * häufig abgefragte Daten).
 *
 * Statt jedes Mal lahm auf die Haupt-DB zuzugreifen, holt sich die App die Daten blitzschnell aus Redis.
 * Das gibt der App 'nen krassen Performance-Boost und sorgt für 'nen smootheren Vibe.
 * Diese Klasse stellt sicher, dass die App weiß, wo Redis läuft und wie sie damit quatschen soll.
 */
@Configuration
@EnableRedisRepositories(basePackages = "org.example.javamusicapp.repository")
public class RedisConfig {

    // Werte aus application.properties injizieren
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    // 1. Manuelle Konfiguration der Redis Connection Factory
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Wir nutzen die Lettuce-Implementierung
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    // 2. Manuelle Konfiguration des RedisTemplate (wie zuvor, aber mit injizierter ConnectionFactory)
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}