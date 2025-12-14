package org.mathieucuvelier.CIViewerCLI.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowRunDTO(Long id, String name, @JsonProperty("head_branch") String headBranch,
        @JsonProperty("head_sha") String headSha, String status, String conclusion,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        @JsonProperty("jobs_url") String jobsUrl) {
    public boolean isAfter(Instant since) {
        return createdAt
                .isAfter(LocalDateTime.ofInstant(since, createdAt.atZone(java.time.ZoneId.systemDefault()).getZone()));
    }

    public Instant getTimestampAsInstant() {
        return createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }
}
