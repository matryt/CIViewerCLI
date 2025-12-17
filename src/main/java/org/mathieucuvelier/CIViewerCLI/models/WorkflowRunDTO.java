package org.mathieucuvelier.CIViewerCLI.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowRunDTO(Long id, String name, @JsonProperty("head_branch") String headBranch,
        @JsonProperty("head_sha") String headSha, String status, String conclusion,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        @JsonProperty("jobs_url") String jobsUrl) {
    public boolean isAfter(ZonedDateTime since) {
        return updatedAt.atZone(ZoneId.of("UTC")).isAfter(since);
    }

    public ZonedDateTime getTimestampAsInstant() {
        return updatedAt.atZone(ZoneId.of("UTC"));
    }
}
