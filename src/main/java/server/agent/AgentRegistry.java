package server.agent;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AgentRegistry {
    private final AgentRepository repository;
    private final Map<Integer, AgentProfile> profilesByCharacterId = new ConcurrentHashMap<>();

    public AgentRegistry(AgentRepository repository) {
        this.repository = repository;
    }

    public void refreshEnabledProfiles() throws SQLException {
        profilesByCharacterId.clear();
        for (AgentProfile profile : repository.findEnabledProfiles()) {
            profilesByCharacterId.put(profile.characterId(), profile);
        }
    }

    public Optional<AgentProfile> findByCharacterId(int characterId) {
        return Optional.ofNullable(profilesByCharacterId.get(characterId));
    }

    public List<AgentProfile> enabledProfiles() {
        return Collections.unmodifiableList(profilesByCharacterId.values().stream()
                .sorted((left, right) -> Integer.compare(left.id(), right.id()))
                .toList());
    }

    public int enabledProfileCount() {
        return profilesByCharacterId.size();
    }
}
