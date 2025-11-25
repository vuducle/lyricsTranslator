package org.example.javamusicapp.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.example.javamusicapp.model.Activity;
import org.example.javamusicapp.model.Nachweis;
import org.example.javamusicapp.model.enums.Weekday;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

@Service
public class PdfExportService {

    private static final String TEMPLATE_PATH = "static/ausbildungsnachweis.pdf";

    public byte[] generateAusbildungsnachweisPdf(Nachweis nachweis) throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        try (InputStream is = resource.getInputStream(); PDDocument document = PDDocument.load(is)) {
            PDAcroForm form = document.getDocumentCatalog().getAcroForm();
            if (form == null)
                throw new IOException("PDF template has no AcroForm fields");

            // Ensure appearances are generated so filled values are visible
            form.setNeedAppearances(true);

            // Fill name and basic fields if present (use exact PDF field names)
            setIfExists(form, "Name", nachweis.getName());
            setIfExists(form, "DatumStart", safeString(nachweis.getDatumStart()));
            setIfExists(form, "DatumEnde", safeString(nachweis.getDatumEnde()));
            setIfExists(form, "Nr", String.valueOf(nachweis.getNummer()));
            setIfExists(form, "Ausbildungsjahr", "2. Ausbildungsjahr"); // optional, set if you store this elsewhere
            setIfExists(form, "ListEvery", null);
            setIfExists(form, "Status", safeString(nachweis.getStatus()));

            // Fill activities: map Weekday -> prefix (Mo, Di, Mi, Do, Fr, Sa, So)
            for (Activity a : nachweis.getActivities()) {
                if (a == null)
                    continue;
                String prefix = prefixForDay(a.getDay());
                if (prefix == null || a.getSlot() == null)
                    continue;
                String slot = String.valueOf(a.getSlot());
                // e.g. Mo_1, Mo_Time_1, Mo_Sec_1
                setIfExists(form, prefix + "_" + slot, safeString(a.getDescription()));
                setIfExists(form, prefix + "_Time_" + slot, safeString(a.getHours()));
                setIfExists(form, prefix + "_Sec_" + slot, safeString(a.getSection()));
            }

            // Totals per day (example: Mo_Total)
            BigDecimal grandTotal = BigDecimal.ZERO;
            for (Weekday day : Weekday.values()) {
                BigDecimal total = nachweis.totalForDay(day);
                String prefix = prefixForDay(day);
                if (prefix != null) {
                    setIfExists(form, prefix + "_Total", safeString(total));
                }
                if (total != null)
                    grandTotal = grandTotal.add(total);
            }

            // Gesamtstunden (summe aller Tage)
            setIfExists(form, "Gesamtstunden", safeString(grandTotal));

            // Signatures / meta
            setIfExists(form, "Remark", null);
            // Ausbilder name
            if (nachweis.getAusbilder() != null) {
                String ausb = nachweis.getAusbilder().getName() != null ? nachweis.getAusbilder().getName()
                        : nachweis.getAusbilder().getUsername();
                setIfExists(form, "Ausbilder", ausb);
            }
            setIfExists(form, "Date_Azubi", safeString(nachweis.getDatumAzubi()));
            setIfExists(form, "Sig_Azubi", safeString(nachweis.getSignaturAzubi()));
            setIfExists(form, "Sig_Ausbilder", safeString(nachweis.getSignaturAusbilder()));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private void setIfExists(PDAcroForm form, String fieldName, Object value) {
        if (value == null)
            return;
        PDField field = form.getField(fieldName);
        if (field != null) {
            try {
                field.setValue(value.toString());
            } catch (IOException e) {
                // log? for now ignore individual field errors
            }
        }
    }

    private String prefixForDay(Weekday day) {
        if (day == null)
            return null;
        switch (day) {
            case MONDAY:
                return "Mo";
            case TUESDAY:
                return "Tu";
            case WEDNESDAY:
                return "We";
            case THURSDAY:
                return "Th";
            case FRIDAY:
                return "Fr";
            case SATURDAY:
                return "Sa";
            case SUNDAY:
                return "Su";
            default:
                return null;
        }
    }

    private String safeString(Object o) {
        if (o == null)
            return null;
        if (o instanceof BigDecimal)
            return ((BigDecimal) o).toString();
        return o.toString();
    }
}
