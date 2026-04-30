package com.homekm.auth;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * RFC 6238 TOTP — HMAC-SHA1, 30-second time step, 6-digit code. Used for
 * second-factor login. Verification accepts ±1 step (so the user can finish
 * typing as the window rolls over).
 *
 * Secret is base32-encoded so it fits the {@code otpauth://} provisioning URI
 * authenticator apps consume.
 */
@Service
public class TotpService {

    private static final int STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int WINDOW = 1;
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final SecureRandom random = new SecureRandom();

    /** 20 random bytes → 32 base32 chars. Matches the RFC 4226 recommendation. */
    public String generateBase32Secret() {
        byte[] buf = new byte[20];
        random.nextBytes(buf);
        return base32Encode(buf);
    }

    /**
     * Build the otpauth URI consumed by Google Authenticator, 1Password, etc.
     * The URI itself is what gets rendered into the QR code on the frontend.
     */
    public String provisioningUri(String issuer, String accountEmail, String base32Secret) {
        String label = urlEncode(issuer + ":" + accountEmail);
        String params = "secret=" + base32Secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + STEP_SECONDS;
        return "otpauth://totp/" + label + "?" + params;
    }

    public boolean verify(String base32Secret, String code) {
        if (code == null || code.length() != DIGITS) return false;
        long currentStep = Instant.now().getEpochSecond() / STEP_SECONDS;
        byte[] key = base32Decode(base32Secret);
        for (int offset = -WINDOW; offset <= WINDOW; offset++) {
            if (compute(key, currentStep + offset).equals(code)) return true;
        }
        return false;
    }

    private String compute(byte[] key, long step) {
        try {
            byte[] msg = ByteBuffer.allocate(8).putLong(step).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(msg);
            int off = hash[hash.length - 1] & 0x0f;
            int bin = ((hash[off] & 0x7f) << 24)
                    | ((hash[off + 1] & 0xff) << 16)
                    | ((hash[off + 2] & 0xff) << 8)
                    | (hash[off + 3] & 0xff);
            int otp = bin % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA1 unavailable", e);
        }
    }

    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int bits = 0, value = 0;
        for (byte b : data) {
            value = (value << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                sb.append(BASE32.charAt((value >>> (bits - 5)) & 0x1f));
                bits -= 5;
            }
        }
        if (bits > 0) {
            sb.append(BASE32.charAt((value << (5 - bits)) & 0x1f));
        }
        return sb.toString();
    }

    private static byte[] base32Decode(String s) {
        s = s.toUpperCase().replaceAll("=", "");
        ByteBuffer buf = ByteBuffer.allocate(s.length() * 5 / 8);
        int bits = 0, value = 0;
        for (char c : s.toCharArray()) {
            int idx = BASE32.indexOf(c);
            if (idx < 0) throw new IllegalArgumentException("invalid base32 char: " + c);
            value = (value << 5) | idx;
            bits += 5;
            if (bits >= 8) {
                buf.put((byte) ((value >>> (bits - 8)) & 0xff));
                bits -= 8;
            }
        }
        byte[] out = new byte[buf.position()];
        buf.flip();
        buf.get(out);
        return out;
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }
}
