package com.homekm.common;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Bcrypt bcrypt = new Bcrypt();
    private Minio minio = new Minio();
    private Vapid vapid = new Vapid();
    private Cors cors = new Cors();
    private Mail mail = new Mail();
    private Trash trash = new Trash();
    private PasswordReset passwordReset = new PasswordReset();
    private RateLimit rateLimit = new RateLimit();
    private Idempotency idempotency = new Idempotency();
    private Files files = new Files();
    private Invitations invitations = new Invitations();
    private Embedding embedding = new Embedding();
    private long presignedUrlExpiryMinutes = 15;
    private String name = "Home KM";

    public static class Jwt {
        private String secret;
        private long expiryHours = 24;
        private int accessExpiryMinutes = 15;
        private int refreshExpiryDays = 30;
        private int refreshExpiryHoursDefault = 8;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpiryHours() { return expiryHours; }
        public void setExpiryHours(long expiryHours) { this.expiryHours = expiryHours; }
        public int getAccessExpiryMinutes() { return accessExpiryMinutes; }
        public void setAccessExpiryMinutes(int accessExpiryMinutes) { this.accessExpiryMinutes = accessExpiryMinutes; }
        public int getRefreshExpiryDays() { return refreshExpiryDays; }
        public void setRefreshExpiryDays(int refreshExpiryDays) { this.refreshExpiryDays = refreshExpiryDays; }
        public int getRefreshExpiryHoursDefault() { return refreshExpiryHoursDefault; }
        public void setRefreshExpiryHoursDefault(int v) { this.refreshExpiryHoursDefault = v; }
    }

    public static class Bcrypt {
        private int cost = 12;

        public int getCost() { return cost; }
        public void setCost(int cost) { this.cost = cost; }
    }

    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName = "homekm";

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getBucketName() { return bucketName; }
        public void setBucketName(String bucketName) { this.bucketName = bucketName; }
    }

    public static class Vapid {
        private String publicKey;
        private String privateKey;
        private String subject;

        public String getPublicKey() { return publicKey; }
        public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
        public String getPrivateKey() { return privateKey; }
        public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";

        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class Trash {
        private int retentionDays = 30;

        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    }

    public static class PasswordReset {
        private int tokenExpiryMinutes = 60;

        public int getTokenExpiryMinutes() { return tokenExpiryMinutes; }
        public void setTokenExpiryMinutes(int tokenExpiryMinutes) { this.tokenExpiryMinutes = tokenExpiryMinutes; }
    }

    public static class Mail {
        private boolean enabled = false;
        private String from = "noreply@homekm.local";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
    }

    public static class RateLimit {
        private boolean enabled = true;
        private List<Rule> rules = new ArrayList<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<Rule> getRules() { return rules; }
        public void setRules(List<Rule> rules) { this.rules = rules; }

        public static class Rule {
            private String id;
            private String pathPattern;
            private String method = "ANY";
            private String scope = "ip";
            private int limit = 60;
            private int windowSeconds = 60;

            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getPathPattern() { return pathPattern; }
            public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }
            public String getMethod() { return method; }
            public void setMethod(String method) { this.method = method; }
            public String getScope() { return scope; }
            public void setScope(String scope) { this.scope = scope; }
            public int getLimit() { return limit; }
            public void setLimit(int limit) { this.limit = limit; }
            public int getWindowSeconds() { return windowSeconds; }
            public void setWindowSeconds(int v) { this.windowSeconds = v; }
        }
    }

    public static class Idempotency {
        private boolean enabled = true;
        private int ttlHours = 24;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getTtlHours() { return ttlHours; }
        public void setTtlHours(int ttlHours) { this.ttlHours = ttlHours; }
    }

    public static class Files {
        private List<String> allowedMime = new ArrayList<>();
        private boolean requireScan = false;
        private String avHost = "";
        private int avPort = 3310;

        public List<String> getAllowedMime() { return allowedMime; }
        public void setAllowedMime(List<String> v) { this.allowedMime = v; }
        public boolean isRequireScan() { return requireScan; }
        public void setRequireScan(boolean requireScan) { this.requireScan = requireScan; }
        public String getAvHost() { return avHost; }
        public void setAvHost(String avHost) { this.avHost = avHost; }
        public int getAvPort() { return avPort; }
        public void setAvPort(int avPort) { this.avPort = avPort; }
    }

    public static class Invitations {
        private boolean allowOpenRegistration = false;
        private int expiryHours = 168;

        public boolean isAllowOpenRegistration() { return allowOpenRegistration; }
        public void setAllowOpenRegistration(boolean v) { this.allowOpenRegistration = v; }
        public int getExpiryHours() { return expiryHours; }
        public void setExpiryHours(int v) { this.expiryHours = v; }
    }

    public static class Embedding {
        private boolean enabled = false;
        private String ollamaUrl = "";
        private String model = "nomic-embed-text";
        private int dim = 1536;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getOllamaUrl() { return ollamaUrl; }
        public void setOllamaUrl(String v) { this.ollamaUrl = v; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDim() { return dim; }
        public void setDim(int dim) { this.dim = dim; }
    }

    @PostConstruct
    void validate() {
        if (jwt.secret == null || jwt.secret.length() < 32) {
            throw new IllegalStateException(
                "app.jwt.secret must be at least 32 characters for HS256; set JWT_SECRET env var");
        }
    }

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Bcrypt getBcrypt() { return bcrypt; }
    public void setBcrypt(Bcrypt bcrypt) { this.bcrypt = bcrypt; }
    public Minio getMinio() { return minio; }
    public void setMinio(Minio minio) { this.minio = minio; }
    public Vapid getVapid() { return vapid; }
    public void setVapid(Vapid vapid) { this.vapid = vapid; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public Mail getMail() { return mail; }
    public void setMail(Mail mail) { this.mail = mail; }
    public Trash getTrash() { return trash; }
    public void setTrash(Trash trash) { this.trash = trash; }
    public PasswordReset getPasswordReset() { return passwordReset; }
    public void setPasswordReset(PasswordReset passwordReset) { this.passwordReset = passwordReset; }
    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
    public Idempotency getIdempotency() { return idempotency; }
    public void setIdempotency(Idempotency idempotency) { this.idempotency = idempotency; }
    public Files getFiles() { return files; }
    public void setFiles(Files files) { this.files = files; }
    public Invitations getInvitations() { return invitations; }
    public void setInvitations(Invitations invitations) { this.invitations = invitations; }
    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }
    public long getPresignedUrlExpiryMinutes() { return presignedUrlExpiryMinutes; }
    public void setPresignedUrlExpiryMinutes(long presignedUrlExpiryMinutes) { this.presignedUrlExpiryMinutes = presignedUrlExpiryMinutes; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
