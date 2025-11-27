package org.example.javamusicapp.controller.internal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.ImageReader;
import java.util.*;

/**
 * 🧐 **Was geht hier ab?**
 * Dieser Controller ist ein reines internes Hilfs-Tool, wahrscheinlich zum Debuggen.
 * Er ist nicht für den normalen User gedacht.
 *
 * Sein Job ist es, Infos über die `ImageIO`-Fähigkeiten der Java-Umgebung rauszuhauen.
 * Er listet auf, welche Bildformate (wie JPG, PNG, WebP) die App lesen und schreiben kann
 * und welche Treiber-Klassen dafür am Start sind.
 *
 * Richtig nützlich, wenn man Probleme mit Bild-Uploads oder -Verarbeitung hat und checken will,
 * ob die richtigen Libraries überhaupt geladen sind.
 */
@RestController
public class ImageIOController {

    @GetMapping("/internal/imageio-writers")
    public Map<String, Object> getImageIOInfo(@RequestParam(required = false) String format) {
        Map<String, Object> result = new LinkedHashMap<>();

        // all available reader/writer format names
        result.put("readerFormatNames", Arrays.asList(ImageIO.getReaderFormatNames()));
        result.put("writerFormatNames", Arrays.asList(ImageIO.getWriterFormatNames()));

        // If a specific format requested, list writer class names and mime types
        if (format != null && !format.isBlank()) {
            String fmt = format.toLowerCase();
            List<String> writerClasses = new ArrayList<>();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(fmt);
            while (writers.hasNext()) {
                writerClasses.add(writers.next().getClass().getName());
            }
            result.put("writersForFormat", writerClasses);

            List<String> readerClasses = new ArrayList<>();
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(fmt);
            while (readers.hasNext()) {
                readerClasses.add(readers.next().getClass().getName());
            }
            result.put("readersForFormat", readerClasses);
        }

        // additionally list writers specifically discovered for common web types
        Map<String, List<String>> byCommon = new LinkedHashMap<>();
        for (String f : new String[] { "webp", "jpg", "jpeg", "png" }) {
            List<String> classes = new ArrayList<>();
            Iterator<ImageWriter> its = ImageIO.getImageWritersByFormatName(f);
            while (its.hasNext())
                classes.add(its.next().getClass().getName());
            byCommon.put(f, classes);
        }
        result.put("commonFormatWriters", byCommon);

        return result;
    }
}
