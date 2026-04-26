package com.homekm.common;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Bcrypt bcrypt = new Bcrypt();
    private Minio minio = new Minio();
    private Vapid vapid = new Vapid();
    private Cors cors = new Cors();
    private long presignedUrlExpiryMinutes = 15;
    private String name = "Home KM";

    public static class Jwt {
        private String secret;
        private long expiryHours = 24;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpiryHours() { return expiryHours; }
        public void setExpiryHours(long expiryHours) { this.expiryHours = expiryHours; }
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
    public long getPresignedUrlExpiryMinutes() { return presignedUrlExpiryMinutes; }
    public void setPresignedUrlExpiryMinutes(long presignedUrlExpiryMinutes) { this.presignedUrlExpiryMinutes = presignedUrlExpiryMinutes; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
