package server.agent;

import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public final class AgentScriptRepository {
    public Optional<AgentScript> findEnabledByName(String name) throws SQLException {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, name, version, enabled, script_type, body, updated_at
                     FROM agent_scripts
                     WHERE name = ? AND enabled = 1
                     ORDER BY version DESC, id DESC
                     LIMIT 1
                     """)) {
            statement.setString(1, name.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AgentScript(
                        result.getInt("id"),
                        result.getString("name"),
                        result.getInt("version"),
                        result.getBoolean("enabled"),
                        result.getString("script_type"),
                        result.getString("body"),
                        nullableInstant(result, "updated_at")
                ));
            }
        }
    }

    private Instant nullableInstant(ResultSet result, String column) throws SQLException {
        Timestamp timestamp = result.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
