package server.agent.actions;

import server.agent.AgentIntentCapability;
import server.agent.AgentIntentType;
import server.agent.AgentNavigationGraphService;
import server.agent.AgentNavigationRoute;
import server.agent.AgentPerceptionSnapshot;
import server.agent.AgentPortalEdge;

public final class AgentNavigationActionAdapter implements AgentActionAdapter {
    private final AgentNavigationGraphService navigationGraphService;

    public AgentNavigationActionAdapter(AgentNavigationGraphService navigationGraphService) {
        this.navigationGraphService = navigationGraphService;
    }

    @Override
    public AgentIntentCapability capability() {
        return AgentIntentCapability.NAVIGATION;
    }

    @Override
    public AgentActionResult execute(AgentActionContext context) {
        AgentIntentType type = context.intent().type();
        if (type == AgentIntentType.MOVE_TO_MAP) {
            return previewMoveToMap(context);
        }
        if (type == AgentIntentType.FOLLOW_CHARACTER) {
            return previewFollowCharacter(context);
        }
        return AgentActionResult.blockedByRuntime(capability(),
                type + " reached the navigation adapter, but movement execution is not implemented yet");
    }

    private AgentActionResult previewMoveToMap(AgentActionContext context) {
        Integer targetMapId = parseMapId(context.intent().argument());
        if (targetMapId == null) {
            return AgentActionResult.blockedByRuntime(capability(), "MOVE_TO_MAP requires a numeric map id");
        }
        if (context.perception() == null || !context.perception().available()) {
            return AgentActionResult.blockedByRuntime(capability(), "Cannot preview route without an available perception snapshot");
        }

        AgentNavigationRoute route = navigationGraphService.findLoadedRoute(
                context.perception().world(),
                context.perception().channel(),
                context.perception().mapId(),
                targetMapId
        );
        if (!route.found()) {
            return AgentActionResult.blockedByRuntime(capability(),
                    "Navigation preview blocked: " + route.message(),
                    routeDetailsJson(route));
        }
        if (route.steps().isEmpty()) {
            return AgentActionResult.ok(capability(), "Navigation target is already the current map", false, routeDetailsJson(route));
        }

        AgentPortalEdge next = route.steps().get(0);
        return AgentActionResult.blockedByRuntime(capability(),
                "Navigation preview found " + route.steps().size() + " loaded portal step(s); next "
                        + next.portalName() + " -> map " + next.toMapId()
                        + ". Gameplay movement remains disabled until the movement adapter is implemented.",
                routeDetailsJson(route));
    }

    private AgentActionResult previewFollowCharacter(AgentActionContext context) {
        String target = context.intent().argument();
        if (target == null || target.isBlank()) {
            return AgentActionResult.blockedByRuntime(capability(), "FOLLOW_CHARACTER requires a character name or id");
        }
        if (context.perception() == null || !context.perception().available()) {
            return AgentActionResult.blockedByRuntime(capability(), "Cannot preview follow behavior without an available perception snapshot");
        }

        AgentPerceptionSnapshot.AgentVisibleObject matched = context.perception().nearbyPlayers().stream()
                .filter(player -> matchesCharacter(player, target))
                .findFirst()
                .orElse(null);
        if (matched == null) {
            return AgentActionResult.blockedByRuntime(
                    capability(),
                    "Follow target '" + target + "' is not visible on the current map. Future movement will need locate-and-route support.",
                    followDetailsJson(context, target, null, "TARGET_NOT_VISIBLE")
            );
        }

        return AgentActionResult.ok(
                capability(),
                "Follow target " + matched.name() + " is visible at distanceSq " + matched.distanceSq()
                        + ". Gameplay movement remains disabled until the movement adapter is implemented.",
                false,
                followDetailsJson(context, target, matched, "TARGET_VISIBLE")
        );
    }

    private boolean matchesCharacter(AgentPerceptionSnapshot.AgentVisibleObject player, String target) {
        String trimmed = target.trim();
        if (player.name() != null && player.name().equalsIgnoreCase(trimmed)) {
            return true;
        }
        if (player.templateId() == null) {
            return false;
        }
        try {
            return player.templateId() == Integer.parseInt(trimmed);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private Integer parseMapId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String routeDetailsJson(AgentNavigationRoute route) {
        StringBuilder builder = new StringBuilder("{");
        builder.append("\"routeState\":\"").append(route.found() ? "READY" : "UNAVAILABLE").append("\",")
                .append("\"world\":").append(route.world()).append(',')
                .append("\"channel\":").append(route.channel()).append(',')
                .append("\"fromMapId\":").append(route.fromMapId()).append(',')
                .append("\"toMapId\":").append(route.toMapId()).append(',')
                .append("\"found\":").append(route.found()).append(',')
                .append("\"message\":\"").append(escapeJson(route.message())).append("\",")
                .append("\"stepCount\":").append(route.steps().size()).append(',')
                .append("\"nextStep\":").append(route.steps().isEmpty() ? "null" : edgeJson(route.steps().get(0))).append(',')
                .append("\"steps\":[");
        for (int i = 0; i < route.steps().size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(edgeJson(route.steps().get(i)));
        }
        return builder.append("]}").toString();
    }

    private String edgeJson(AgentPortalEdge edge) {
        return "{"
                + "\"fromMapId\":" + edge.fromMapId() + ","
                + "\"fromMapName\":\"" + escapeJson(edge.fromMapName()) + "\","
                + "\"portalId\":" + edge.portalId() + ","
                + "\"portalName\":\"" + escapeJson(edge.portalName()) + "\","
                + "\"portalType\":" + edge.portalType() + ","
                + "\"position\":{\"x\":" + edge.x() + ",\"y\":" + edge.y() + "},"
                + "\"toMapId\":" + edge.toMapId() + ","
                + "\"targetPortalName\":\"" + escapeJson(edge.targetPortalName()) + "\","
                + "\"open\":" + edge.open() + ","
                + "\"scripted\":" + edge.scripted()
                + "}";
    }

    private String followDetailsJson(
            AgentActionContext context,
            String requestedTarget,
            AgentPerceptionSnapshot.AgentVisibleObject matched,
            String state
    ) {
        return "{"
                + "\"followState\":\"" + state + "\","
                + "\"world\":" + context.perception().world() + ","
                + "\"channel\":" + context.perception().channel() + ","
                + "\"mapId\":" + context.perception().mapId() + ","
                + "\"agentPosition\":{\"x\":" + context.perception().x() + ",\"y\":" + context.perception().y() + "},"
                + "\"requestedTarget\":\"" + escapeJson(requestedTarget) + "\","
                + "\"visiblePlayers\":" + context.perception().nearbyPlayers().size() + ","
                + "\"target\":" + (matched == null ? "null" : visiblePlayerJson(matched))
                + "}";
    }

    private String visiblePlayerJson(AgentPerceptionSnapshot.AgentVisibleObject player) {
        return "{"
                + "\"characterId\":" + player.templateId() + ","
                + "\"objectId\":" + player.objectId() + ","
                + "\"name\":\"" + escapeJson(player.name()) + "\","
                + "\"level\":" + nullableNumber(player.level()) + ","
                + "\"position\":{\"x\":" + player.x() + ",\"y\":" + player.y() + "},"
                + "\"distanceSq\":" + player.distanceSq()
                + "}";
    }

    private String nullableNumber(Integer value) {
        return value == null ? "null" : value.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
