package com.homekm.file;

import com.homekm.common.AppProperties;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Service
public class MimeService {

    private final Tika tika = new Tika();
    private final AppProperties appProperties;

    public MimeService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String detect(InputStream stream, String filename) throws IOException {
        return tika.detect(stream, filename);
    }

    public void enforceAllowed(String detected, String declared) {
        Set<String> allowed = Set.copyOf(appProperties.getFiles().getAllowedMime());
        if (allowed.isEmpty()) return;
        String d = detected != null ? detected.toLowerCase() : "";
        if (!allowed.contains(d)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "INVALID_FILE_TYPE: " + detected);
        }
        if (declared != null && !declared.isBlank()) {
            String declLower = declared.toLowerCase();
            if (!declLower.equals(d) && !mediaTypePrefixMatches(declLower, d)) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "MIME_MISMATCH: declared=" + declared + " detected=" + detected);
            }
        }
    }

    private boolean mediaTypePrefixMatches(String declared, String detected) {
        // Allow declared `application/octet-stream` (browser default) only if detected is allowlisted.
        return "application/octet-stream".equals(declared);
    }
}
