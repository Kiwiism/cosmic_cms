package server.agent;

import java.util.List;

public record AgentNavigationRoute(
        int world,
        int channel,
        int fromMapId,
        int toMapId,
        boolean found,
        String message,
        List<AgentPortalEdge> steps
) {
    public AgentNavigationRoute {
        steps = List.copyOf(steps);
    }

    public static AgentNavigationRoute notFound(int world, int channel, int fromMapId, int toMapId, String message) {
        return new AgentNavigationRoute(world, channel, fromMapId, toMapId, false, message, List.of());
    }
}
