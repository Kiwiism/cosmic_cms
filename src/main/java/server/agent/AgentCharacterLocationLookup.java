package server.agent;

import client.Character;
import net.server.Server;
import net.server.world.World;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class AgentCharacterLocationLookup {
    public Optional<LocatedCharacter> find(String characterIdOrName) {
        if (characterIdOrName == null || characterIdOrName.isBlank()) {
            return Optional.empty();
        }

        String target = characterIdOrName.trim();
        Optional<LocatedCharacter> online = findOnline(target);
        if (online.isPresent()) {
            return online;
        }

        try {
            return findSaved(target);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    private Optional<LocatedCharacter> findOnline(String target) {
        Server server = Server.getInstance();
        for (World world : server.getWorlds()) {
            Character character = parseInt(target)
                    .map(id -> world.getPlayerStorage().getCharacterById(id))
                    .orElseGet(() -> world.getPlayerStorage().getCharacterByName(target));
            if (character == null || character.getMap() == null || character.getClient() == null) {
                continue;
            }

            return Optional.of(new LocatedCharacter(
                    character.getId(),
                    character.getName(),
                    character.getWorld(),
                    character.getClient().getChannel(),
                    character.getMapId(),
                    character.getPosition().x,
                    character.getPosition().y,
                    true
            ));
        }
        return Optional.empty();
    }

    private Optional<LocatedCharacter> findSaved(String target) throws SQLException {
        String sql = parseInt(target).isPresent()
                ? "SELECT id, name, world, map, spawnpoint FROM characters WHERE id=? LIMIT 1"
                : "SELECT id, name, world, map, spawnpoint FROM characters WHERE name=? LIMIT 1";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Optional<Integer> id = parseInt(target);
            if (id.isPresent()) {
                statement.setInt(1, id.get());
            } else {
                statement.setString(1, target);
            }

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                return Optional.of(new LocatedCharacter(
                        result.getInt("id"),
                        result.getString("name"),
                        result.getInt("world"),
                        1,
                        result.getInt("map"),
                        0,
                        result.getInt("spawnpoint"),
                        false
                ));
            }
        }
    }

    private Optional<Integer> parseInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public record LocatedCharacter(
            int characterId,
            String name,
            int world,
            int channel,
            int mapId,
            int x,
            int y,
            boolean online
    ) {
    }
}
