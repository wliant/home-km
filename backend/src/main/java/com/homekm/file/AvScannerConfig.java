package com.homekm.file;

import com.homekm.common.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

@Configuration
public class AvScannerConfig {

    private static final Logger log = LoggerFactory.getLogger(AvScannerConfig.class);

    @Bean
    public AvScanner avScanner(AppProperties props) {
        AppProperties.Files files = props.getFiles();
        if (files.getAvHost() == null || files.getAvHost().isBlank()) {
            log.info("AV scan: NoopAvScanner (set FILES_REQUIRE_SCAN=true and CLAMD_HOST to enable ClamAV)");
            return new NoopAvScanner();
        }
        log.info("AV scan: ClamdAvScanner targeting {}:{}", files.getAvHost(), files.getAvPort());
        return new ClamdAvScanner(files.getAvHost(), files.getAvPort());
    }

    static class NoopAvScanner implements AvScanner {
        @Override public ScanResult scan(InputStream content) { return ScanResult.clean(); }
    }

    /** Minimal clamd "INSTREAM" client — sufficient for opt-in scanning. */
    static class ClamdAvScanner implements AvScanner {
        private final String host;
        private final int port;

        ClamdAvScanner(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public ScanResult scan(InputStream content) {
            try (Socket s = new Socket(host, port);
                 OutputStream out = s.getOutputStream();
                 InputStream in = s.getInputStream()) {
                s.setSoTimeout(30_000);
                out.write("zINSTREAM\0".getBytes());
                byte[] buf = new byte[8192];
                int n;
                while ((n = content.read(buf)) >= 0) {
                    out.write(intBe(n));
                    if (n > 0) out.write(buf, 0, n);
                }
                out.write(intBe(0));
                out.flush();
                String response = new String(in.readAllBytes()).trim();
                if (response.endsWith("OK")) return ScanResult.clean();
                if (response.contains("FOUND")) {
                    int colon = response.indexOf(':');
                    int found = response.lastIndexOf("FOUND");
                    String name = (colon >= 0 && found > colon) ? response.substring(colon + 1, found).trim() : response;
                    return ScanResult.infected(name);
                }
                return ScanResult.error(response);
            } catch (IOException e) {
                return ScanResult.error(e.getMessage());
            }
        }

        private static byte[] intBe(int n) {
            return new byte[]{(byte) (n >>> 24), (byte) (n >>> 16), (byte) (n >>> 8), (byte) n};
        }
    }
}
