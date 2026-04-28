package com.homekm.note;

import com.homekm.auth.UserPrincipal;
import com.homekm.common.EntityNotFoundException;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/notes")
public class NoteExportController {

    private final NoteRepository noteRepository;

    public NoteExportController(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id,
                                          @RequestParam(defaultValue = "md") String format,
                                          @AuthenticationPrincipal UserPrincipal principal) throws Exception {
        Note n = noteRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Note", id));

        String safeName = n.getTitle().replaceAll("[^a-zA-Z0-9._-]", "_");

        if ("md".equalsIgnoreCase(format)) {
            String md = "# " + n.getTitle() + "\n\n" + (n.getBody() == null ? "" : n.getBody());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + ".md\"")
                    .contentType(MediaType.parseMediaType("text/markdown"))
                    .body(md.getBytes(StandardCharsets.UTF_8));
        }
        if ("pdf".equalsIgnoreCase(format)) {
            String html = renderHtml(n);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());
        }
        return ResponseEntity.badRequest().build();
    }

    private String renderHtml(Note n) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String body = renderer.render(parser.parse(n.getBody() == null ? "" : n.getBody()));
        return """
                <!DOCTYPE html><html><head><meta charset="UTF-8">
                <style>
                  body { font-family: 'Helvetica', sans-serif; line-height: 1.5; color: #111; padding: 24px; }
                  h1 { border-bottom: 1px solid #ccc; padding-bottom: 8px; }
                  pre, code { background: #f6f8fa; padding: 2px 4px; border-radius: 4px; }
                </style></head><body>
                """
                + "<h1>" + escape(n.getTitle()) + "</h1>"
                + body
                + "</body></html>";
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
