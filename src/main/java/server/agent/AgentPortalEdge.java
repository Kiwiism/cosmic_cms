package server.agent;

public record AgentPortalEdge(
        int world,
        int channel,
        int fromMapId,
        String fromMapName,
        int portalId,
        String portalName,
        int portalType,
        int x,
        int y,
        int toMapId,
        String targetPortalName,
        boolean open,
        boolean scripted
) {
}
