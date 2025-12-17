package org.mathieucuvelier.CIViewerCLI.persistence;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public record MonitorState(
    ZonedDateTime lastCheckTimestamp,
    Map<Long, RunState> knownRuns
) {
    public static MonitorState empty() {
        return new MonitorState(LocalDateTime.MIN.atZone(ZoneId.systemDefault()), new HashMap<>());
    }
}
