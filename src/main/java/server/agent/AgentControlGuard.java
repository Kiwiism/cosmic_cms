package server.agent;

import client.Character;
import client.Client;
import net.server.Server;
import net.server.world.World;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Central safety check before future agent code may control a character.
 *
 * The guard is intentionally conservative. It refuses control when a real
 * client may already own the account or character.
 */
public final class AgentControlGuard {
    public AgentControlDecision canRuntimeControl(AgentProfile profile) {
        if (!profile.enabled()) {
            return AgentControlDecision.denied(
                    AgentControlDenyReason.PROFILE_DISABLED,
                    "Agent profile is disabled"
            );
        }

        AgentControlDecision liveDecision = checkLivePlayerStorage(profile.characterId());
        if (!liveDecision.allowed()) {
            return liveDecision;
        }

        return checkDatabaseLoginState(profile.characterId());
    }

    private AgentControlDecision checkLivePlayerStorage(int characterId) {
        Server server = Server.getInstance();
        for (World world : server.getWorlds()) {
            Character character = world.getPlayerStorage().getCharacterById(characterId);
            if (character == null) {
                continue;
            }
            if (character.isLoggedin()) {
                return AgentControlDecision.denied(
                        AgentControlDenyReason.CHARACTER_ONLINE,
                        "Character is already present in live world storage"
                );
            }
        }
        return AgentControlDecision.allowed(-1, -1);
    }

    private AgentControlDecision checkDatabaseLoginState(int characterId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT c.world, a.loggedin
                     FROM characters c
                     JOIN accounts a ON a.id = c.accountid
                     WHERE c.id = ?
                     """)) {
            statement.setInt(1, characterId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return AgentControlDecision.denied(
                            AgentControlDenyReason.CHARACTER_NOT_FOUND,
                            "Character no longer exists"
                    );
                }

                int loginState = result.getInt("loggedin");
                if (loginState != Client.LOGIN_NOTLOGGEDIN) {
                    return AgentControlDecision.denied(
                            AgentControlDenyReason.ACCOUNT_BUSY,
                            "Account is logged in or in transition"
                    );
                }

                return AgentControlDecision.allowed(result.getInt("world"), 1);
            }
        } catch (SQLException e) {
            return AgentControlDecision.denied(
                    AgentControlDenyReason.DATABASE_UNAVAILABLE,
                    "Could not verify account login state"
            );
        }
    }
}
