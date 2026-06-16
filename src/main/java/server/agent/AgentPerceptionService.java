package server.agent;

import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import server.maps.MapleMap;

import java.time.Instant;

/**
 * Read-only environment snapshot service for future agents.
 *
 * The initial snapshot deliberately exposes counts only. Detailed object lists,
 * pathing, target selection, and combat awareness can be layered on later once
 * the control runtime exists.
 */
public final class AgentPerceptionService {
    public AgentPerceptionSnapshot snapshot(AgentManagedCharacter managed) {
        MapleMap map = managed.character().getMap();
        if (map == null) {
            return AgentPerceptionSnapshot.unavailable(managed.spawnPlan(), "Agent character is not attached to a map");
        }

        return new AgentPerceptionSnapshot(
                true,
                managed.spawnPlan().world(),
                managed.spawnPlan().channel(),
                map.getId(),
                map.countPlayers(),
                map.countMonsters(),
                map.countItems(),
                map.countReactors(),
                "Entered agent map snapshot captured",
                Instant.now()
        );
    }

    public AgentPerceptionSnapshot snapshot(AgentSpawnPlan plan) {
        if (!plan.ready()) {
            return AgentPerceptionSnapshot.unavailable(plan, plan.controlDecision().message());
        }

        try {
            World world = Server.getInstance().getWorld(plan.world());
            if (world == null) {
                return AgentPerceptionSnapshot.unavailable(plan, "World is not loaded");
            }

            Channel channel = world.getChannel(plan.channel());
            if (channel == null) {
                return AgentPerceptionSnapshot.unavailable(plan, "Channel is not loaded");
            }

            MapleMap map = channel.getMapFactory().getMap(plan.mapId());
            if (map == null) {
                return AgentPerceptionSnapshot.unavailable(plan, "Map is not loaded");
            }

            return new AgentPerceptionSnapshot(
                    true,
                    plan.world(),
                    plan.channel(),
                    plan.mapId(),
                    map.countPlayers(),
                    map.countMonsters(),
                    map.countItems(),
                    map.countReactors(),
                    "Map snapshot captured",
                    Instant.now()
            );
        } catch (RuntimeException e) {
            return AgentPerceptionSnapshot.unavailable(plan, "Unable to capture map snapshot: " + e.getMessage());
        }
    }
}
