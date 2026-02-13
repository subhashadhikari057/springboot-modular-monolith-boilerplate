package com.starterpack.backend.config;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiDocsUiController {

    @GetMapping(value = "/api-docs/admin", produces = MediaType.TEXT_HTML_VALUE)
    public String adminDocs() {
        return scalarHtml("Admin API Docs", "/openapi/admin", "/favicon-admin.svg");
    }

    @GetMapping(value = "/api-docs/mobile", produces = MediaType.TEXT_HTML_VALUE)
    public String mobileDocs() {
        return scalarHtml("Mobile API Docs", "/openapi/mobile", "/favicon-mobile.svg");
    }

    private String scalarHtml(String title, String specUrl, String faviconPath) {
        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>%s</title>
                  <link rel="icon" href="%s" type="image/svg+xml" />
                </head>
                <body>
                  <script id="api-reference" data-url="%s"></script>
                  <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
                </body>
                </html>
                """.formatted(title, faviconPath, specUrl);
    }
}
