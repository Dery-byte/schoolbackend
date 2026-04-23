package com.alibou.book.utillities;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class SubjectNormalizer {

    private static final Map<String, String> ALIASES = new LinkedHashMap<>() {{
        put("ENGLISH LANG", "ENGLISH LANGUAGE");
        put("MATHS", "MATHEMATICS(CORE)");
        put("MATHEMATICS", "MATHEMATICS(CORE)");
        put("SOCIAL STUDY", "SOCIAL STUDIES");
        put("INTEGRATED SCI", "INTEGRATED SCIENCE");
    }};

    // Regex patterns
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Z0-9() ]");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

    public String normalize(String subject) {
        if (subject == null) return "";

        // Normalize Unicode characters (removes accents and weird spacing)
        String clean = Normalizer.normalize(subject, Normalizer.Form.NFKC)
                .toUpperCase()
                .trim();

        // Remove punctuation and invisible Unicode characters
        clean = NON_ALPHANUMERIC.matcher(clean).replaceAll("");
        clean = MULTIPLE_SPACES.matcher(clean).replaceAll(" ");

        // Remove stray spaces before parentheses (e.g. "MATHEMATICS (CORE)" → "MATHEMATICS(CORE)")
        clean = clean.replace(" (", "(").replace(") ", ")");

        // Apply alias mapping if found
        for (Map.Entry<String, String> entry : ALIASES.entrySet()) {
            if (clean.equals(entry.getKey())) {
                return entry.getValue();
            }
        }

        return clean;
    }
}
