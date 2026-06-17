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

    public long longPolicy(int agentProfileId, String policyKey, long fallback) throws SQLException {
        Optional<String> perAgent = readRawPolicyValue(agentProfileId, policyKey);
        if (perAgent.isPresent()) {
            return parseLong(perAgent.get(), fallback);
        }

        Optional<String> global = readRawPolicyValue(0, policyKey);
        return global.map(value -> parseLong(value, fallback)).orElse(fallback);
    }

    private Optional<Boolean> readPolicyValue(int agentProfileId, String policyKey) throws SQLException {
        return readRawPolicyValue(agentProfileId, policyKey).map(this::parseBoolean);
    }

    private Optional<String> readRawPolicyValue(int agentProfileId, String policyKey) throws SQLException {
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
                return Optional.ofNullable(result.getString("policy_value"));
            }
        }
    }

    private long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(0L, Long.parseLong(value.trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
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
