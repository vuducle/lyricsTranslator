package org.example.javamusicapp.service.misc;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Simple YouTube captions fetcher using YouTube Data API (search) and the
 * timedtext endpoint.
 * Note: timedtext is not guaranteed to exist for every video; this is a
 * best-effort fallback.
 */
@Service
public class YouTubeService {
    private static final Logger LOG = LoggerFactory.getLogger(YouTubeService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${youtube.api.key:}")
    private String youtubeApiKey;

    /**
     * Search YouTube by title+artist and try to fetch captions for the best hit.
     * 
     * @param title  song title
     * @param artist artist name
     * @param lang   preferred caption language (e.g. "vi" or "en"); may be
     *               null/empty
     * @return optional plain-text captions (joined transcript) or empty if none
     *         found
     */
    public Optional<String> fetchCaptionsByTitleArtist(String title, String artist, String lang) {
        if (youtubeApiKey == null || youtubeApiKey.isBlank()) {
            LOG.warn("YouTube API key not configured (youtube.api.key)");
            return Optional.empty();
        }

        try {
            String q = String.format("%s %s lyrics", title == null ? "" : title, artist == null ? "" : artist).trim();
            String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8);
            String url = String.format(
                    "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=5&q=%s&key=%s",
                    encoded, youtubeApiKey);

            Map resp = restTemplate.getForObject(url, Map.class);
            if (resp == null)
                return Optional.empty();

            List items = (List) resp.get("items");
            if (items == null || items.isEmpty())
                return Optional.empty();

            for (Object o : items) {
                if (!(o instanceof Map))
                    continue;
                Map item = (Map) o;
                Object idObj = item.get("id");
                if (!(idObj instanceof Map))
                    continue;
                Map id = (Map) idObj;
                Object videoIdObj = id.get("videoId");
                if (videoIdObj == null)
                    continue;
                String videoId = videoIdObj.toString();
                Optional<String> caps = fetchCaptionsByVideoId(videoId, lang);
                if (caps.isPresent())
                    return caps;
            }

            return Optional.empty();
        } catch (Exception e) {
            LOG.warn("YouTube search/captions fetch failed", e);
            return Optional.empty();
        }
    }

    /**
     * Try timedtext endpoint for several language fallbacks.
     */
    public Optional<String> fetchCaptionsByVideoId(String videoId, String preferredLang) {
        if (videoId == null || videoId.isBlank())
            return Optional.empty();

        LOG.info("fetchCaptionsByVideoId called for videoId='{}' preferredLang='{}'", videoId, preferredLang);

        String[] langs;
        if (preferredLang == null || preferredLang.isBlank()) {
            langs = new String[] { "en", "vi" };
        } else {
            langs = new String[] { preferredLang, "en", "vi" };
        }

        for (String lang : langs) {
            try {
                String timedUrl = String.format("https://www.youtube.com/api/timedtext?lang=%s&v=%s",
                        URLEncoder.encode(lang, StandardCharsets.UTF_8),
                        URLEncoder.encode(videoId, StandardCharsets.UTF_8));
                String body = restTemplate.getForObject(timedUrl, String.class);
                if (body == null || body.isBlank())
                    continue;

                LOG.info("timedtext returned {} bytes for video {} lang {}", body.length(), videoId, lang);

                // Parse XML transcript using Jsoup XML parser and join <text> nodes
                Document doc = Jsoup.parse(body, "", Parser.xmlParser());
                Elements texts = doc.select("text");
                if (texts == null || texts.isEmpty())
                    continue;
                StringBuilder sb = new StringBuilder();
                for (Element t : texts) {
                    String txt = t.text();
                    if (txt != null && !txt.isBlank()) {
                        sb.append(txt.trim()).append('\n');
                    }
                }
                String joined = sb.toString().trim();
                if (!joined.isBlank())
                    return Optional.of(joined);
            } catch (Exception e) {
                LOG.debug("timedtext fetch for video {} lang {} failed: {}", videoId, lang, e.getMessage());
            }
        }

        // If timedtext didn't yield captions, try yt-dlp as a fallback (if available)
        try {
            LOG.info("timedtext did not produce captions for video {}; attempting yt-dlp fallback", videoId);
            Optional<String> fromYtdlp = fetchCaptionsWithYtDlp(videoId, langs);
            if (fromYtdlp.isPresent())
                return fromYtdlp;
        } catch (Exception e) {
            LOG.debug("yt-dlp fallback failed for {}: {}", videoId, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Fallback: call external `yt-dlp` binary to download subtitles (auto or
     * manual) into a temp dir,
     * parse the resulting .srt/.vtt file and return the text.
     */
    public Optional<String> fetchCaptionsWithYtDlp(String videoId, String[] langs) throws Exception {
        // Check yt-dlp availability
        ProcessBuilder whichPb = new ProcessBuilder("which", "yt-dlp");
        Process which = whichPb.start();
        String whichOut = new String(which.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        int whichExit = which.waitFor();
        if (whichExit != 0) {
            LOG.info("yt-dlp not found on PATH; skipping yt-dlp fallback");
            return Optional.empty();
        }
        LOG.info("yt-dlp found at: {}", whichOut);

        java.nio.file.Path tmpDir = java.nio.file.Files.createTempDirectory("ytcaps_");
        try {
            String url = "https://www.youtube.com/watch?v=" + videoId;

            // First, run `yt-dlp --list-subs` to detect available subtitle codes.
            List<String> availableCodes = new java.util.ArrayList<>();
            {
                List<String> listCmd = java.util.Arrays.asList("yt-dlp", "--no-warnings", "--skip-download",
                        "--list-subs", url);
                ProcessBuilder listPb = new ProcessBuilder(listCmd);
                listPb.redirectErrorStream(true);
                Process lp = listPb.start();
                String listOut = new String(lp.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                lp.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
                LOG.debug("yt-dlp --list-subs output length {}", listOut.length());
                LOG.info("yt-dlp --list-subs output (first 2k): {}",
                        listOut.length() > 2000 ? listOut.substring(0, 2000) : listOut);

                // Parse first-token codes from lines that look like: "en-nP7-2PuUl7o English -
                // en vtt, srt, ..."
                String[] lines = listOut.split("\\r?\\n");
                for (String L : lines) {
                    String t = L.trim();
                    if (t.isEmpty())
                        continue;
                    // skip header lines
                    if (t.toLowerCase().startsWith("available"))
                        continue;
                    // token is first whitespace-separated column
                    String[] parts = t.split("\\s+", 2);
                    if (parts.length >= 1) {
                        String code = parts[0].trim();
                        // crude filter: code should contain a dash or be short language code
                        if (code.matches("[a-zA-Z0-9_-]{2,40}")) {
                            availableCodes.add(code);
                        }
                    }
                }
                LOG.info("Detected subtitle codes: {}", availableCodes);
            }

            // prefer codes that match requested langs
            java.util.List<String> triedCmds = new java.util.ArrayList<>();
            if (!availableCodes.isEmpty()) {
                for (String pref : langs) {
                    if (pref == null || pref.isBlank())
                        continue;
                    // find a code that starts with pref or contains -pref-
                    Optional<String> pick = availableCodes.stream()
                            .filter(c -> c.equalsIgnoreCase(pref) || c.startsWith(pref + "-")
                                    || c.contains("-" + pref + "-") || c.endsWith("-" + pref))
                            .findFirst();
                    if (pick.isPresent()) {
                        String code = pick.get();
                        // download that specific code
                        List<String> cmd = java.util.Arrays.asList(
                                "yt-dlp",
                                "--no-warnings",
                                "--skip-download",
                                "--write-auto-sub",
                                "--write-sub",
                                "--sub-lang", code,
                                "--sub-format", "srt/vtt/best",
                                "-o", tmpDir.resolve("%(id)s.%(ext)s").toString(),
                                url);
                        triedCmds.add(String.join(" ", cmd));
                        ProcessBuilder pb = new ProcessBuilder(cmd);
                        pb.redirectErrorStream(true);
                        Process p = pb.start();
                        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                        p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
                        LOG.debug("yt-dlp download output len {}", out.length());
                        LOG.info("yt-dlp download output (first 2k): {}",
                                out.length() > 2000 ? out.substring(0, 2000) : out);

                        try (java.util.stream.Stream<java.nio.file.Path> s = java.nio.file.Files.list(tmpDir)) {
                            java.util.Optional<java.nio.file.Path> opt = s
                                    .filter(pth -> pth.getFileName().toString().startsWith(videoId + "."))
                                    .filter(pth -> {
                                        String nm = pth.getFileName().toString().toLowerCase();
                                        return nm.endsWith(".srt") || nm.endsWith(".vtt") || nm.endsWith(".xml")
                                                || nm.endsWith(".txt");
                                    })
                                    .findFirst();

                            if (opt.isPresent()) {
                                java.nio.file.Path file = opt.get();
                                String content = java.nio.file.Files.readString(file, StandardCharsets.UTF_8);
                                String cleaned = cleanupSubtitleContent(content);
                                return Optional.of(cleaned);
                            }
                        }
                    }
                }

                // If nothing matched preferred codes, try the first available code
                String first = availableCodes.get(0);
                List<String> cmd = java.util.Arrays.asList(
                        "yt-dlp",
                        "--no-warnings",
                        "--skip-download",
                        "--write-auto-sub",
                        "--write-sub",
                        "--sub-lang", first,
                        "--sub-format", "srt/vtt/best",
                        "-o", tmpDir.resolve("%(id)s.%(ext)s").toString(),
                        url);
                triedCmds.add(String.join(" ", cmd));
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
                LOG.debug("yt-dlp download output len {}", out.length());
                LOG.info("yt-dlp download output (first 2k): {}", out.length() > 2000 ? out.substring(0, 2000) : out);

                try (java.util.stream.Stream<java.nio.file.Path> s = java.nio.file.Files.list(tmpDir)) {
                    java.util.Optional<java.nio.file.Path> opt = s
                            .filter(pth -> pth.getFileName().toString().startsWith(videoId + "."))
                            .filter(pth -> {
                                String nm = pth.getFileName().toString().toLowerCase();
                                return nm.endsWith(".srt") || nm.endsWith(".vtt") || nm.endsWith(".xml")
                                        || nm.endsWith(".txt");
                            })
                            .findFirst();

                    if (opt.isPresent()) {
                        java.nio.file.Path file = opt.get();
                        String content = java.nio.file.Files.readString(file, StandardCharsets.UTF_8);
                        String cleaned = cleanupSubtitleContent(content);
                        return Optional.of(cleaned);
                    }
                }
            }

            // As a last resort try --all-subs (download any available subtitles)
            List<String> allCmd = java.util.Arrays.asList(
                    "yt-dlp",
                    "--no-warnings",
                    "--skip-download",
                    "--all-subs",
                    "--write-auto-sub",
                    "--sub-format", "srt/vtt/best",
                    "-o", tmpDir.resolve("%(id)s.%(ext)s").toString(),
                    url);
            triedCmds.add(String.join(" ", allCmd));
            ProcessBuilder allPb = new ProcessBuilder(allCmd);
            allPb.redirectErrorStream(true);
            Process ap = allPb.start();
            String allOut = new String(ap.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            ap.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
            LOG.debug("yt-dlp --all-subs output len {}", allOut.length());
            LOG.info("yt-dlp --all-subs output (first 2k): {}",
                    allOut.length() > 2000 ? allOut.substring(0, 2000) : allOut);

            try (java.util.stream.Stream<java.nio.file.Path> s = java.nio.file.Files.list(tmpDir)) {
                java.util.Optional<java.nio.file.Path> opt = s
                        .filter(pth -> pth.getFileName().toString().startsWith(videoId + "."))
                        .filter(pth -> {
                            String nm = pth.getFileName().toString().toLowerCase();
                            return nm.endsWith(".srt") || nm.endsWith(".vtt") || nm.endsWith(".xml")
                                    || nm.endsWith(".txt");
                        })
                        .findFirst();

                if (opt.isPresent()) {
                    java.nio.file.Path file = opt.get();
                    String content = java.nio.file.Files.readString(file, StandardCharsets.UTF_8);
                    String cleaned = cleanupSubtitleContent(content);
                    return Optional.of(cleaned);
                }
            }

            LOG.info("yt-dlp tried commands: {}", triedCmds);
            return Optional.empty();
        } finally {
            // best-effort cleanup
            try {
                java.nio.file.Files.walk(tmpDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                java.nio.file.Files.deleteIfExists(p);
                            } catch (Exception ignored) {
                            }
                        });
            } catch (Exception ignored) {
            }
        }
    }

    private String cleanupSubtitleContent(String content) {
        // Remove SRT numeric indices and timestamp lines
        StringBuilder sb = new StringBuilder();
        String[] lines = content.split("\\r?\\n");
        for (String l : lines) {
            String t = l.trim();
            if (t.isBlank()) {
                sb.append('\n');
                continue;
            }
            // skip pure numeric lines
            if (t.matches("^\\d+$"))
                continue;
            // skip timestamp lines like 00:00:01,200 --> 00:00:03,400 or 00:00:01.200 -->
            // 00:00:03.400
            if (t.matches("^\\d{2}:\\d{2}:\\d{2}[\\.,]\\d{3}\\s+-->.*$"))
                continue;
            // remove possible WEBVTT or NOTE header
            if (t.toLowerCase().startsWith("webvtt") || t.toLowerCase().startsWith("note"))
                continue;
            sb.append(t).append('\n');
        }
        // collapse multiple blank lines
        String joined = sb.toString().replaceAll("\\n{2,}", "\\n").trim();
        return joined;
    }
}
