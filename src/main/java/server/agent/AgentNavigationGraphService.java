package server.agent;

import constants.id.MapId;
import net.server.Server;
import net.server.channel.Channel;
import server.maps.MapleMap;
import server.maps.Portal;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Builds a conservative portal graph from maps that are already loaded.
 *
 * This avoids forcing WZ map loads during agent planning. Missing routes mean
 * "not visible in the currently loaded map cache", not "impossible in-game".
 */
public final class AgentNavigationGraphService {
    private static final int DEFAULT_MAX_DEPTH = 30;

    public AgentNavigationRoute findLoadedRoute(int world, int channel, int fromMapId, int toMapId) {
        return findLoadedRoute(world, channel, fromMapId, toMapId, DEFAULT_MAX_DEPTH);
    }

    public AgentNavigationRoute findLoadedRoute(int world, int channel, int fromMapId, int toMapId, int maxDepth) {
        if (fromMapId == toMapId) {
            return new AgentNavigationRoute(world, channel, fromMapId, toMapId, true, "Already on target map", List.of());
        }

        Channel channelServer = Server.getInstance().getChannel(world, channel);
        if (channelServer == null || channelServer.getMapFactory() == null) {
            return AgentNavigationRoute.notFound(world, channel, fromMapId, toMapId, "World/channel is not loaded");
        }

        Map<Integer, MapleMap> loadedMaps = channelServer.getMapFactory().getMaps();
        if (!loadedMaps.containsKey(fromMapId)) {
            return AgentNavigationRoute.notFound(world, channel, fromMapId, toMapId, "Current map is not loaded in channel cache");
        }
        if (!loadedMaps.containsKey(toMapId)) {
            return AgentNavigationRoute.notFound(world, channel, fromMapId, toMapId, "Target map is not loaded in channel cache");
        }

        Map<Integer, List<AgentPortalEdge>> graph = loadedEdges(world, channel, loadedMaps);
        Queue<RouteNode> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        Map<Integer, AgentPortalEdge> parentEdgeByMap = new HashMap<>();

        queue.add(new RouteNode(fromMapId, 0));
        visited.add(fromMapId);

        while (!queue.isEmpty()) {
            RouteNode current = queue.poll();
            if (current.depth() >= maxDepth) {
                continue;
            }

            for (AgentPortalEdge edge : graph.getOrDefault(current.mapId(), List.of())) {
                if (!visited.add(edge.toMapId())) {
                    continue;
                }

                parentEdgeByMap.put(edge.toMapId(), edge);
                if (edge.toMapId() == toMapId) {
                    List<AgentPortalEdge> steps = reconstructRoute(fromMapId, toMapId, parentEdgeByMap);
                    return new AgentNavigationRoute(world, channel, fromMapId, toMapId, true,
                            "Route found through loaded portal graph", steps);
                }
                queue.add(new RouteNode(edge.toMapId(), current.depth() + 1));
            }
        }

        return AgentNavigationRoute.notFound(world, channel, fromMapId, toMapId,
                "No route found through currently loaded non-door portals within depth " + maxDepth);
    }

    public Map<Integer, List<AgentPortalEdge>> loadedEdges(int world, int channel, Map<Integer, MapleMap> loadedMaps) {
        Map<Integer, List<AgentPortalEdge>> edgesByMap = new LinkedHashMap<>();
        for (MapleMap map : loadedMaps.values()) {
            List<AgentPortalEdge> edges = new ArrayList<>();
            for (Portal portal : map.getPortals()) {
                if (!isTraversalPortal(portal)) {
                    continue;
                }

                int targetMapId = portal.getTargetMapId();
                if (!loadedMaps.containsKey(targetMapId)) {
                    continue;
                }

                Point position = portal.getPosition();
                edges.add(new AgentPortalEdge(
                        world,
                        channel,
                        map.getId(),
                        map.getMapName(),
                        portal.getId(),
                        portal.getName(),
                        portal.getType(),
                        position == null ? 0 : position.x,
                        position == null ? 0 : position.y,
                        targetMapId,
                        portal.getTarget(),
                        portal.getPortalStatus(),
                        portal.getScriptName() != null && !portal.getScriptName().isBlank()
                ));
            }
            edgesByMap.put(map.getId(), List.copyOf(edges));
        }
        return Map.copyOf(edgesByMap);
    }

    private boolean isTraversalPortal(Portal portal) {
        int targetMapId = portal.getTargetMapId();
        if (targetMapId == MapId.NONE || targetMapId < 0) {
            return false;
        }
        return portal.getType() == Portal.MAP_PORTAL || portal.getType() == Portal.TELEPORT_PORTAL;
    }

    private List<AgentPortalEdge> reconstructRoute(
            int fromMapId,
            int toMapId,
            Map<Integer, AgentPortalEdge> parentEdgeByMap
    ) {
        LinkedList<AgentPortalEdge> steps = new LinkedList<>();
        int currentMapId = toMapId;
        while (currentMapId != fromMapId) {
            AgentPortalEdge edge = parentEdgeByMap.get(currentMapId);
            if (edge == null) {
                return List.of();
            }
            steps.addFirst(edge);
            currentMapId = edge.fromMapId();
        }
        return List.copyOf(steps);
    }

    private record RouteNode(int mapId, int depth) {
    }
}
