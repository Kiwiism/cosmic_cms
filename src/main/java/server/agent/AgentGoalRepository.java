package server.agent;

import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public final class AgentGoalRepository {
    public void recordPlanningTick(
            AgentGoal goal,
            AgentIntent intent,
            AgentIntentDispatchResult dispatchResult,
            AgentPerceptionSnapshot perception,
            AgentKnowledgeSnapshot knowledge,
            AgentGoalProgressDecision progressDecision,
            String plannerReason
    ) throws SQLException {
        AgentGoalDiagnosis diagnosis = diagnoseGoal(intent, dispatchResult, progressDecision);
        String progressJson = """
                {"lastIntent":"%s","lastArgument":%s,"dispatchStatus":"%s","dispatchMessage":%s,"policyAllowed":%s,"capability":"%s","goalStatus":"%s","goalTerminal":%s,"goalReason":%s,"diagnosisState":"%s","diagnosisReason":%s,"recommendedAction":%s,"level":%d,"job":%d,"world":%d,"channel":%d,"mapId":%d,"x":%d,"y":%d,"players":%d,"monsters":%d,"drops":%d,"npcs":%d,"reactors":%d,"plannerReason":%s}
                """.formatted(
                escapeJson(intent.type().name()),
                nullableString(intent.argument()),
                escapeJson(dispatchResult.status().name()),
                nullableString(dispatchResult.message()),
                dispatchResult.policyAllowed(),
                escapeJson(dispatchResult.capability().name()),
                escapeJson(progressDecision.nextStatus()),
                progressDecision.terminal(),
                nullableString(progressDecision.reason()),
                escapeJson(diagnosis.state()),
                nullableString(diagnosis.reason()),
                nullableString(diagnosis.recommendedAction()),
                knowledge.level(),
                knowledge.jobId(),
                perception.world(),
                perception.channel(),
                perception.mapId(),
                perception.x(),
                perception.y(),
                perception.players(),
                perception.monsters(),
                perception.drops(),
                perception.npcs(),
                perception.reactors(),
                nullableString(plannerReason)
        ).strip();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE agent_goals
                     SET status = CASE
                             WHEN ? IN ('COMPLETED', 'FAILED', 'CANCELLED') THEN ?
                             WHEN status IN ('PENDING', 'ACTIVE') THEN 'RUNNING'
                             ELSE status
                         END,
                         started_at = COALESCE(started_at, CURRENT_TIMESTAMP),
                         completed_at = CASE
                             WHEN ? IN ('COMPLETED', 'FAILED', 'CANCELLED') THEN COALESCE(completed_at, CURRENT_TIMESTAMP)
                             ELSE completed_at
                         END,
                         progress_json = ?,
                         updated_at = CURRENT_TIMESTAMP
                     WHERE id = ?
                       AND agent_profile_id = ?
                       AND status IN ('PENDING', 'ACTIVE', 'RUNNING')
                     """)) {
            statement.setString(1, progressDecision.nextStatus());
            statement.setString(2, progressDecision.nextStatus());
            statement.setString(3, progressDecision.nextStatus());
            statement.setString(4, progressJson);
            statement.setLong(5, goal.id());
            statement.setInt(6, goal.agentProfileId());
            statement.executeUpdate();
        }
    }

    private AgentGoalDiagnosis diagnoseGoal(
            AgentIntent intent,
            AgentIntentDispatchResult dispatchResult,
            AgentGoalProgressDecision progressDecision
    ) {
        if (progressDecision.terminal()) {
            if ("COMPLETED".equals(progressDecision.nextStatus())) {
                return new AgentGoalDiagnosis(
                        "COMPLETED",
                        progressDecision.reason(),
                        "No action needed; the goal reached a terminal completed state."
                );
            }
            return new AgentGoalDiagnosis(
                    "FAILED",
                    progressDecision.reason(),
                    "Inspect the latest INTENT_DISPATCH log, adjust the goal, then reactivate or recreate it."
            );
        }

        if (dispatchResult.status() == AgentActionStatus.FAILED) {
            return new AgentGoalDiagnosis(
                    "FAILED",
                    dispatchResult.message(),
                    "Runtime execution failed. Check server logs and the latest dispatch details before retrying."
            );
        }

        if (dispatchResult.status() == AgentActionStatus.BLOCKED && !dispatchResult.policyAllowed()) {
            return new AgentGoalDiagnosis(
                    "BLOCKED_BY_POLICY",
                    dispatchResult.message(),
                    "Enable the " + dispatchResult.capability() + " capability policy for this agent if this behavior is intended."
            );
        }

        if (dispatchResult.status() == AgentActionStatus.BLOCKED && isCooldownBlock(dispatchResult.detailsJson())) {
            return new AgentGoalDiagnosis(
                    "WAITING_FOR_COOLDOWN",
                    dispatchResult.message(),
                    "Wait for the cooldown to expire or lower the relevant cooldown in Agent CMS."
            );
        }

        if (dispatchResult.status() == AgentActionStatus.BLOCKED) {
            return new AgentGoalDiagnosis(
                    "BLOCKED_BY_RUNTIME",
                    dispatchResult.message(),
                    "This intent passed policy but the runtime adapter could not execute it yet. Check whether this capability is implemented."
            );
        }

        if (dispatchResult.status() == AgentActionStatus.DENIED) {
            return new AgentGoalDiagnosis(
                    "DENIED",
                    dispatchResult.message(),
                    "Review the runtime guard or action validation that denied this intent."
            );
        }

        return new AgentGoalDiagnosis(
                "RUNNING",
                progressDecision.reason(),
                intent.type() == AgentIntentType.UNKNOWN
                        ? "Clarify or fix the script line so it maps to a supported intent."
                        : "No immediate action needed; the goal remains active."
        );
    }

    private boolean isCooldownBlock(String detailsJson) {
        return detailsJson != null && detailsJson.contains("\"cooldownState\":\"BLOCKED\"");
    }

    public Optional<AgentGoal> findNextActiveGoal(int agentProfileId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, agent_profile_id, goal_type, priority, status, target_world, target_channel,
                            target_map, target_ref, parameters_json, progress_json, created_at, updated_at,
                            started_at, completed_at
                     FROM agent_goals
                     WHERE agent_profile_id = ?
                       AND status IN ('PENDING', 'ACTIVE', 'RUNNING')
                     ORDER BY priority DESC, id ASC
                     LIMIT 1
                     """)) {
            statement.setInt(1, agentProfileId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(readGoal(result));
            }
        }
    }

    private AgentGoal readGoal(ResultSet result) throws SQLException {
        return new AgentGoal(
                result.getLong("id"),
                result.getInt("agent_profile_id"),
                result.getString("goal_type"),
                result.getInt("priority"),
                result.getString("status"),
                nullableInt(result, "target_world"),
                nullableInt(result, "target_channel"),
                nullableInt(result, "target_map"),
                result.getString("target_ref"),
                result.getString("parameters_json"),
                result.getString("progress_json"),
                nullableInstant(result, "created_at"),
                nullableInstant(result, "updated_at"),
                nullableInstant(result, "started_at"),
                nullableInstant(result, "completed_at")
        );
    }

    private Integer nullableInt(ResultSet result, String column) throws SQLException {
        int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }

    private Instant nullableInstant(ResultSet result, String column) throws SQLException {
        Timestamp timestamp = result.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String nullableString(String value) {
        return value == null ? "null" : "\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private record AgentGoalDiagnosis(String state, String reason, String recommendedAction) {}
}
