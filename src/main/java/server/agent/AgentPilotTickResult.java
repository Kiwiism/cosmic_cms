package server.agent;

import java.time.Instant;

public record AgentPilotTickResult(
        int profileId,
        long runtimeSessionId,
        AgentIntent intent,
        AgentIntentDispatchResult dispatchResult,
        AgentPerceptionSnapshot perception,
        String scriptSource,
        String message,
        Instant tickedAt
) {
}
