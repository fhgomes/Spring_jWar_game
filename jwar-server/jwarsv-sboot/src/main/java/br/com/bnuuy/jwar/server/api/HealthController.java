package br.com.bnuuy.jwar.server.api;

import br.com.bnuuy.jwar.server.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "System", description = "Health and meta endpoints")
public class HealthController {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Autowired(required = false)
    private GitProperties gitProperties;

    @Operation(summary = "Liveness/readiness health summary")
    @GetMapping
    public HealthResponse health() {
        String version = buildProperties != null ? buildProperties.getVersion() : "dev";
        String gitCommit = gitProperties != null ? gitProperties.getShortCommitId() : "unknown";
        return new HealthResponse("UP", version, gitCommit, Instant.now());
    }
}
