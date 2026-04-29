package com.homekm.common;

import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public-readable subset of /actuator/info exposed via /api/info so the
 * frontend (which only proxies /api/* through nginx) can render the running
 * version on Settings → About without authentication.
 *
 * Both {@link BuildProperties} and {@link GitProperties} are autowired by
 * Spring Boot when META-INF/build-info.properties and BOOT-INF/classes/
 * git.properties exist on the classpath — the gradle-git-properties plugin
 * + springBoot.buildInfo() in build.gradle.kts emit both.
 */
@RestController
@RequestMapping("/api")
public class BuildInfoController {

    private final BuildProperties build;
    private final GitProperties git;

    public BuildInfoController(BuildProperties build, GitProperties git) {
        this.build = build;
        this.git = git;
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        if (build != null) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("name", build.getName());
            b.put("version", build.getVersion());
            b.put("time", build.getTime() != null ? build.getTime().toString() : null);
            body.put("build", b);
        }
        if (git != null) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("branch", git.getBranch());
            g.put("commitId", git.getShortCommitId());
            g.put("commitTime", git.get("commit.time"));
            g.put("tags", git.get("tags"));
            body.put("git", g);
        }
        return ResponseEntity.ok(body);
    }
}
