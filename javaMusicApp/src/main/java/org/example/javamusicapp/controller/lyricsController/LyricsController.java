package org.example.javamusicapp.controller.lyricsController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.javamusicapp.controller.lyricsController.dto.LyricsDTO;
import org.example.javamusicapp.service.misc.LyricsService;
import org.example.javamusicapp.service.misc.YouTubeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lyrics")
@RequiredArgsConstructor
@Tag(name = "Lyrics", description = "Endpoints for translating and processing song lyrics")
public class LyricsController {

        private static final Logger LOG = LoggerFactory.getLogger(LyricsController.class);

        private final LyricsService lyricsService;
        private final YouTubeService youTubeService;

        @PostMapping("/translate")
        @Operation(summary = "Translate lyrics", description = "Translate provided lyrics from source language to target language (e.g. VI -> EN)")
        public ResponseEntity<?> translate(@RequestBody LyricsDTO request) {
                // Validate input: if no original lyrics and no title/artist for lookup -> 422
                boolean hasOriginal = request.getOriginalLyrics() != null && !request.getOriginalLyrics().isBlank();
                boolean hasTitleOrArtist = (request.getTitle() != null && !request.getTitle().isBlank())
                                || (request.getArtist() != null && !request.getArtist().isBlank());
                if (!hasOriginal && !hasTitleOrArtist) {
                        return ResponseEntity.unprocessableEntity().body(
                                        new org.example.javamusicapp.controller.lyricsController.dto.ErrorResponse(
                                                        java.time.Instant.now(), 422, "Unprocessable Entity",
                                                        "Provide 'originalLyrics' or 'title'/'artist' for lookup"));
                }

                java.util.Optional<LyricsDTO> resultOpt = lyricsService.translate(request);
                if (resultOpt.isEmpty()) {
                        return ResponseEntity.status(404).body(
                                        new org.example.javamusicapp.controller.lyricsController.dto.ErrorResponse(
                                                        java.time.Instant.now(), 404, "Not Found",
                                                        "Lyrics could not be found"));
                }

                return ResponseEntity.ok(resultOpt.get());
        }

        @GetMapping("/youtube")
        @Operation(summary = "Fetch captions from YouTube", description = "Search YouTube by title/artist or use a videoId and return captions if available", responses = {
                        @ApiResponse(responseCode = "200", description = "YouTube captions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LyricsDTO.class)))
        })
        public ResponseEntity<?> youtubeLookup(
                        @org.springframework.web.bind.annotation.RequestParam(required = false) String title,
                        @org.springframework.web.bind.annotation.RequestParam(required = false) String artist,
                        @org.springframework.web.bind.annotation.RequestParam(required = false) String videoId,
                        @org.springframework.web.bind.annotation.RequestParam(required = false) String lang) {

                LOG.info("youtubeLookup called with videoId='{}' title='{}' artist='{}' lang='{}'", videoId, title,
                                artist, lang);

                if ((videoId == null || videoId.isBlank()) && (title == null || title.isBlank())
                                && (artist == null || artist.isBlank())) {
                        return ResponseEntity.unprocessableEntity().body(
                                        new org.example.javamusicapp.controller.lyricsController.dto.ErrorResponse(
                                                        java.time.Instant.now(), 422, "Unprocessable Entity",
                                                        "Provide 'videoId' or 'title'/'artist' for YouTube lookup"));
                }

                java.util.Optional<String> capsOpt = java.util.Optional.empty();
                if (videoId != null && !videoId.isBlank()) {
                        LOG.info("Calling youTubeService.fetchCaptionsByVideoId videoId={} lang={}", videoId, lang);
                        capsOpt = youTubeService.fetchCaptionsByVideoId(videoId, lang);
                        LOG.info("fetchCaptionsByVideoId returned {}", capsOpt.isPresent() ? "present" : "empty");
                }
                if (capsOpt.isEmpty()) {
                        LOG.info("Falling back to youTubeService.fetchCaptionsByTitleArtist title='{}' artist='{}' lang={}",
                                        title, artist, lang);
                        capsOpt = youTubeService.fetchCaptionsByTitleArtist(title, artist, lang);
                        LOG.info("fetchCaptionsByTitleArtist returned {}", capsOpt.isPresent() ? "present" : "empty");
                }

                if (capsOpt.isEmpty()) {
                        return ResponseEntity.status(404).body(
                                        new org.example.javamusicapp.controller.lyricsController.dto.ErrorResponse(
                                                        java.time.Instant.now(), 404, "Not Found",
                                                        "Captions not found on YouTube"));
                }

                String caps = capsOpt.get();
                // basic cleanup: remove repeated blank lines
                String cleaned = caps.replaceAll("(?m)^[ \t]+|[ \t]+$", "").replaceAll("\n{2,}", "\n");

                LyricsDTO dto = LyricsDTO.builder()
                                .title(title)
                                .artist(artist)
                                .originalLyrics(cleaned)
                                .sourceLang(lang == null ? "" : lang)
                                .targetLang("en")
                                .translatedLyrics(null)
                                .build();

                return ResponseEntity.ok(dto);
        }

}
