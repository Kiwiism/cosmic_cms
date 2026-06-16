package server.agent;

import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

public final class AgentPolicyRepository {
    public boolean isCapabilityEnabled(int agentProfileId, AgentIntentCapability capability) throws SQLException {
        Optional<Boolean> perAgent = readPolicyValue(agentProfileId, capability.policyKey());
        if (perAgent.isPresent()) {
            return perAgent.get();
        }

        Optional<Boolean> global = readPolicyValue(0, capability.policyKey());
        return global.orElse(capability.defaultEnabled());
    }

    private Optional<Boolean> readPolicyValue(int agentProfileId, String policyKey) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT policy_value
                     FROM agent_policies
                     WHERE agent_profile_id = ? AND policy_key = ?
                     ORDER BY id DESC
                     LIMIT 1
                     """)) {
            statement.setInt(1, agentProfileId);
            statement.setString(2, policyKey);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(parseBoolean(result.getString("policy_value")));
            }
        }
    }

    private boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "1", "true", "yes", "y", "enabled", "on" -> true;
            default -> false;
        };
    }
}
