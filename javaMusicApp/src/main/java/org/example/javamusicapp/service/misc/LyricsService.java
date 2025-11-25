package org.example.javamusicapp.service.misc;

import org.example.javamusicapp.controller.lyricsController.dto.DeepLReponseDTO;
import org.example.javamusicapp.controller.lyricsController.dto.LyricsDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class LyricsService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final YouTubeService youTubeService;

    @Value("${deepl.api.key:}")
    private String deepLApiKey;

    public LyricsService(YouTubeService youTubeService) {
        this.youTubeService = youTubeService;
    }

    /**
     * Translate provided lyrics using DeepL API.
     * If `sourceLang` is null or empty, DeepL will auto-detect.
     * Returns Optional.empty() when lyrics cannot be obtained (neither provided nor
     * fetched).
     */
    public java.util.Optional<LyricsDTO> translate(LyricsDTO request) {
        String original = request.getOriginalLyrics();
        if (original == null || original.isBlank()) {
            // try to fetch from YouTube captions using title/artist
            String title = request.getTitle();
            String artist = request.getArtist();
            if ((title != null && !title.isBlank()) || (artist != null && !artist.isBlank())) {
                String preferredLang = request.getSourceLang();
                original = youTubeService.fetchCaptionsByTitleArtist(title, artist, preferredLang).orElse(null);
            }
            if (original == null || original.isBlank()) {
                return java.util.Optional.empty();
            }
        }

        String source = request.getSourceLang();
        String target = request.getTargetLang();
        if (target == null || target.isBlank()) {
            target = "EN"; // default to English
        }

        // Use DeepL API (form encoded)
        String url = "https://api-free.deepl.com/v2/translate"; // works with API key

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("auth_key", deepLApiKey);
        body.add("text", original);
        body.add("target_lang", target.toUpperCase());
        if (source != null && !source.isBlank()) {
            body.add("source_lang", source.toUpperCase());
        }

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        DeepLReponseDTO resp = restTemplate.postForObject(url, entity, DeepLReponseDTO.class);

        String translated = null;
        if (resp != null && resp.getTranslations() != null && !resp.getTranslations().isEmpty()) {
            translated = resp.getTranslations().get(0).getText();
        }

        LyricsDTO dto = LyricsDTO.builder()
                .originalLyrics(original)
                .translatedLyrics(translated == null ? "" : translated)
                .title(request.getTitle())
                .artist(request.getArtist())
                .sourceLang(source)
                .targetLang(target)
                .build();

        return java.util.Optional.of(dto);
    }
}
