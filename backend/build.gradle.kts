plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.owasp.dependencycheck") version "9.0.10"
    id("info.solidsoft.pitest") version "1.15.0"
    id("org.cyclonedx.bom") version "1.10.0"
}

group = "com.homekm"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

// Pin transitive deps so the same commit produces the same classpath months
// apart. Lock files live under gradle/dependency-locks/. Refresh after an
// intentional dep change with: ./gradlew dependencies --write-locks
dependencyLocking {
    lockAllConfigurations()
    lockMode = LockMode.STRICT
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Observability — Prometheus exposition + JSON-structured logs
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Database
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.postgresql:postgresql")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // MinIO
    implementation("io.minio:minio:8.5.7")

    // Web Push (VAPID)
    implementation("nl.martijndwars:web-push:5.1.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")

    // Caching
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-reactor:2.2.0")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    // MIME detection
    implementation("org.apache.tika:tika-core:2.9.2")

    // Markdown + PDF (notes export, attachments)
    implementation("org.commonmark:commonmark:0.21.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.21.0")
    implementation("org.xhtmlrenderer:flying-saucer-pdf:9.5.1")

    // ICS (calendar export)
    implementation("org.mnode.ical4j:ical4j:3.2.14")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:toxiproxy")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

dependencyCheck {
    failBuildOnCVSS = 7.0f
    suppressionFile = "dependency-check-suppressions.xml"
    formats = listOf("HTML", "SARIF")
    nvd.apiKey = System.getenv("NVD_API_KEY") ?: ""
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

// Mutation testing — runs weekly via .github/workflows/mutation-testing.yml.
// Skipped on every-PR runs because Pitest is slow (~5–10 min per package).
pitest {
    junit5PluginVersion = "1.2.1"
    pitestVersion = "1.16.1"
    targetClasses = listOf(
        "com.homekm.auth.*",
        "com.homekm.common.*",
        "com.homekm.file.*",
    )
    threads = 4
    outputFormats = listOf("HTML", "XML")
    timestampedReports = false
    mutationThreshold = 70
    coverageThreshold = 70
    avoidCallsTo = listOf(
        "kotlin.jvm.internal",
        "java.util.logging",
        "org.slf4j",
    )
}

// OpenAPI contract drift is checked via OpenApiContractTest (integration test).
// To regenerate the committed baseline at backend/openapi.yaml, run:
//   UPDATE_OPENAPI_BASELINE=1 ./gradlew test --tests "*OpenApiContractTest"
// The test fetches /v3/api-docs.yaml from a Testcontainers-backed Spring app.

// CycloneDX SBOM — emit at build time alongside the boot jar so release CI
// can attach it to the GitHub release. Run: ./gradlew cyclonedxBom
tasks.named("cyclonedxBom") {
    // Skip dev-only configurations to keep the SBOM focused on shipped deps.
    setProperty("includeConfigs", listOf("runtimeClasspath"))
    setProperty("skipConfigs", listOf("testCompileClasspath", "testRuntimeClasspath"))
    setProperty("outputFormat", "json")
    setProperty("outputName", "sbom-backend")
}
