package server.agent;

import constants.id.MapId;
import net.server.Server;
import net.server.channel.Channel;
import server.maps.MapleMap;
import server.maps.MapManager;
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
    private static final int DEFAULT_EXPANDED_MAX_DEPTH = 20;
    private static final int DEFAULT_EXPANDED_MAP_LIMIT = 64;

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

    public AgentNavigationRoute findBoundedRoute(int world, int channel, int fromMapId, int toMapId) {
        return findBoundedRoute(world, channel, fromMapId, toMapId, DEFAULT_EXPANDED_MAX_DEPTH, DEFAULT_EXPANDED_MAP_LIMIT);
    }

    public AgentNavigationRoute findBoundedRoute(int world, int channel, int fromMapId, int toMapId, int maxDepth, int maxLoadedMaps) {
        if (fromMapId == toMapId) {
            return new AgentNavigationRoute(world, channel, fromMapId, toMapId, true, "Already on target map", List.of());
        }

        Channel channelServer = Server.getInstance().getChannel(world, channel);
        if (channelServer == null || channelServer.getMapFactory() == null) {
            return AgentNavigationRoute.notFound(world, channel, fromMapId, toMapId, "World/channel is not loaded");
        }

        MapManager mapManager = channelServer.getMapFactory();
        Map<Integer, MapleMap> routeMaps = new LinkedHashMap<>(mapManager.getMaps());
        MapleMap startMap = loadRouteMap(mapManager, routeMaps, fromMapId, maxLoadedMaps);
        if (startMap == null) {
            return AgentNavigationRoute.notFound(world, channel, fromMapId, toMapId, "Current map could not be loaded for bounded route expansion");
        }
        loadRouteMap(mapManager, routeMaps, toMapId, maxLoadedMaps);

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

            MapleMap currentMap = loadRouteMap(mapManager, routeMaps, current.mapId(), maxLoadedMaps);
            if (currentMap == null) {
                continue;
            }

            for (AgentPortalEdge edge : expandableEdges(world, channel, mapManager, routeMaps, currentMap, maxLoadedMaps)) {
                if (!visited.add(edge.toMapId())) {
                    continue;
                }

                parentEdgeByMap.put(edge.toMapId(), edge);
                if (edge.toMapId() == toMapId) {
                    List<AgentPortalEdge> steps = reconstructRoute(fromMapId, toMapId, parentEdgeByMap);
                    return new AgentNavigationRoute(world, channel, fromMapId, toMapId, true,
                            "Route found through bounded portal graph expansion with " + routeMaps.size() + " loaded map(s)", steps);
                }
                queue.add(new RouteNode(edge.toMapId(), current.depth() + 1));
            }
        }

        return AgentNavigationRoute.notFound(world, channel, fromMapId, toMapId,
                "No route found through bounded non-scripted portal expansion within depth " + maxDepth
                        + " and map limit " + maxLoadedMaps + "; explored " + routeMaps.size() + " map(s)");
    }

    public Map<Integer, List<AgentPortalEdge>> loadedEdges(int world, int channel, Map<Integer, MapleMap> loadedMaps) {
        Map<Integer, List<AgentPortalEdge>> edgesByMap = new LinkedHashMap<>();
        for (MapleMap map : loadedMaps.values()) {
            List<AgentPortalEdge> edges = new ArrayList<>();
            for (Portal portal : map.getPortals()) {
                if (!isSafeGraphPortal(portal)) {
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

    private List<AgentPortalEdge> expandableEdges(
            int world,
            int channel,
            MapManager mapManager,
            Map<Integer, MapleMap> routeMaps,
            MapleMap map,
            int maxLoadedMaps
    ) {
        List<AgentPortalEdge> edges = new ArrayList<>();
        for (Portal portal : map.getPortals()) {
            if (!isSafeGraphPortal(portal)) {
                continue;
            }

            int targetMapId = portal.getTargetMapId();
            MapleMap targetMap = loadRouteMap(mapManager, routeMaps, targetMapId, maxLoadedMaps);
            if (targetMap == null) {
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
                    targetMap.getId(),
                    portal.getTarget(),
                    portal.getPortalStatus(),
                    false
            ));
        }
        return List.copyOf(edges);
    }

    private MapleMap loadRouteMap(MapManager mapManager, Map<Integer, MapleMap> routeMaps, int mapId, int maxLoadedMaps) {
        MapleMap cached = routeMaps.get(mapId);
        if (cached != null) {
            return cached;
        }
        if (routeMaps.size() >= maxLoadedMaps) {
            return null;
        }
        try {
            MapleMap loaded = mapManager.getMap(mapId);
            if (loaded != null) {
                routeMaps.put(mapId, loaded);
            }
            return loaded;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private boolean isTraversalPortal(Portal portal) {
        int targetMapId = portal.getTargetMapId();
        if (targetMapId == MapId.NONE || targetMapId < 0) {
            return false;
        }
        return portal.getType() == Portal.MAP_PORTAL || portal.getType() == Portal.TELEPORT_PORTAL;
    }

    private boolean isSafeGraphPortal(Portal portal) {
        return isTraversalPortal(portal)
                && portal.getPortalStatus()
                && (portal.getScriptName() == null || portal.getScriptName().isBlank());
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
