package admin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.ShopFactory;
import server.life.MonsterInformationProvider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Private, allowlisted bridge used by the Database CMS for live-only operations.
 *
 * The bridge intentionally has no arbitrary SQL, command, or reflection endpoint. Set
 * COSMIC_BRIDGE_TOKEN to enable it; bind address and port default to 127.0.0.1:8787.
 */
public final class CmsBridgeServer {
    private static final Logger log = LoggerFactory.getLogger(CmsBridgeServer.class);
    private static final Map<String, String> DOTENV = loadDotenv();
    private final String token;
    private HttpServer server;
    private ExecutorService executor;

    public CmsBridgeServer(String token) {
        this.token = token;
    }

    public static CmsBridgeServer fromEnvironment() {
        String token = env("COSMIC_BRIDGE_TOKEN");
        return token == null || token.isBlank() ? null : new CmsBridgeServer(token);
    }

    public void start() {
        BridgeEndpoint endpoint = bridgeEndpoint();
        try {
            server = HttpServer.create(new InetSocketAddress(endpoint.host(), endpoint.port()), 0);
            executor = Executors.newVirtualThreadPerTaskExecutor();
            server.setExecutor(executor);
            server.createContext("/internal/admin/health", exchange -> handle(exchange, "GET", this::health));
            server.createContext("/internal/admin/cache/drops/reload",
                    exchange -> handle(exchange, "POST", this::reloadDrops));
            server.createContext("/internal/admin/cache/shops/reload",
                    exchange -> handle(exchange, "POST", this::reloadShops));
            server.start();
            log.info("CMS bridge listening on {}:{}", endpoint.host(), endpoint.port());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start CMS bridge", exception);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            server = null;
        }
        if (executor != null) {
            executor.close();
            executor = null;
        }
    }

    private void handle(HttpExchange exchange, String method, Handler handler) throws IOException {
        try (exchange) {
            if (!method.equals(exchange.getRequestMethod())) {
                respond(exchange, 405, "{\"error\":\"method_not_allowed\"}");
                return;
            }
            if (!authorized(exchange)) {
                respond(exchange, 401, "{\"error\":\"unauthorized\"}");
                return;
            }
            handler.handle(exchange);
        } catch (Exception exception) {
            log.warn("CMS bridge request failed", exception);
            respond(exchange, 500, "{\"error\":\"internal_error\"}");
        }
    }

    private boolean authorized(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        byte[] supplied = authorization.substring(7).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), supplied);
    }

    private void health(HttpExchange exchange) throws IOException {
        Server cosmic = Server.getInstance();
        int onlinePlayers = cosmic.getWorlds().stream()
                .mapToInt(world -> world.getPlayerStorage().getSize())
                .sum();
        String json = """
                {"status":"UP","instanceId":"%s","startedAt":"%s","worlds":%d,"channels":%d,"onlinePlayers":%d}
                """.formatted(Long.toString(Server.uptime), Instant.ofEpochMilli(Server.uptime),
                cosmic.getWorldsSize(), cosmic.getAllChannels().size(), onlinePlayers).trim();
        respond(exchange, 200, json);
    }

    private void reloadDrops(HttpExchange exchange) throws IOException {
        MonsterInformationProvider.getInstance().clearDrops();
        respond(exchange, 200, "{\"reloaded\":true,\"cache\":\"drops\"}");
    }

    private void reloadShops(HttpExchange exchange) throws IOException {
        ShopFactory.getInstance().reloadShops();
        respond(exchange, 200, "{\"reloaded\":true,\"cache\":\"shops\"}");
    }

    private void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    private String json(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static BridgeEndpoint bridgeEndpoint() {
        String host = env("COSMIC_BRIDGE_HOST");
        String port = env("COSMIC_BRIDGE_PORT");
        String url = env("COSMIC_BRIDGE_URL");
        if ((host == null || host.isBlank() || port == null || port.isBlank())
                && url != null && !url.isBlank()) {
            try {
                URI uri = URI.create(url);
                if (host == null || host.isBlank()) {
                    host = uri.getHost();
                }
                if (port == null || port.isBlank()) {
                    port = uri.getPort() > 0 ? Integer.toString(uri.getPort()) : null;
                }
            } catch (IllegalArgumentException ignored) {
                // Use the default loopback endpoint when COSMIC_BRIDGE_URL is malformed.
            }
        }

        return new BridgeEndpoint(
                host == null || host.isBlank() ? "127.0.0.1" : host,
                port == null || port.isBlank() ? 8787 : Integer.parseInt(port));
    }

    private static String env(String name) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return DOTENV.get(name);
    }

    private static Map<String, String> loadDotenv() {
        Path path = findDotenv();
        if (!Files.isRegularFile(path)) {
            return Map.of();
        }

        Map<String, String> values = new HashMap<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#")) {
                    continue;
                }

                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException exception) {
            log.warn("Unable to read .env for CMS bridge fallback", exception);
        }
        return Map.copyOf(values);
    }

    private static Path findDotenv() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of(".env");
    }

    private record BridgeEndpoint(String host, int port) {}

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
