package com.cosmic.servercms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/worlds")
public class WorldController {
    private static final String[] WORLD_NAMES = {
            "Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan", "Mardia",
            "Kradia", "Yellonde", "Demethos", "Galicia", "El Nido", "Zenith",
            "Arcenia", "Kastia", "Judis", "Plana", "Kalluna", "Stius", "Croa", "Medere"
    };
    private static final Pattern ACTIVE_WORLDS = Pattern.compile("^\\s*WORLDS:\\s*(\\d+)", Pattern.MULTILINE);
    private static final Pattern WORLD_ENTRY = Pattern.compile("^\\s{2}-\\s+flag:", Pattern.MULTILINE);
    private final JdbcTemplate jdbc;
    private final Path configFile;

    public WorldController(JdbcTemplate jdbc, @Value("${cosmic.project-path:.}") String projectPath) {
        this.jdbc = jdbc;
        configFile = Path.of(projectPath).toAbsolutePath().normalize().resolve("config.yaml");
    }

    @GetMapping
    List<Map<String, Object>> worlds() {
        String yaml = readConfig();
        int configuredCount = countWorldEntries(yaml);
        int activeCount = effectiveWorldCount(yaml);
        int count = Math.min(WORLD_NAMES.length, Math.max(configuredCount, activeCount));
        List<Map<String, Object>> worlds = new ArrayList<>(count);
        for (int id = 0; id < count; id++) {
            Map<String, Object> world = new LinkedHashMap<>();
            world.put("id", id);
            world.put("name", WORLD_NAMES[id]);
            world.put("active", id < activeCount);
            worlds.add(world);
        }
        return worlds;
    }

    private String readConfig() {
        try {
            return Files.readString(configFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read Cosmic worlds from " + configFile, exception);
        }
    }

    private int activeWorldCount(String yaml) {
        Matcher matcher = ACTIVE_WORLDS.matcher(yaml);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
    }

    private int effectiveWorldCount(String yaml) {
        Integer override = jdbc.query("""
                        SELECT CAST(value_text AS SIGNED)
                        FROM setting_overrides
                        WHERE setting_key = 'server.worlds.count'
                          AND scope_type = 'GLOBAL'
                          AND scope_id = 0
                          AND active = 1
                        LIMIT 1
                        """,
                resultSet -> resultSet.next() ? resultSet.getInt(1) : null);
        return override != null ? override : activeWorldCount(yaml);
    }

    private int countWorldEntries(String yaml) {
        int count = 0;
        Matcher matcher = WORLD_ENTRY.matcher(yaml);
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
