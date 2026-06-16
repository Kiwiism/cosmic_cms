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
}
