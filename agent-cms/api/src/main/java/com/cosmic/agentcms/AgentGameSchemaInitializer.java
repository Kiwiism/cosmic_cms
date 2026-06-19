package com.cosmic.agentcms;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AgentGameSchemaInitializer {
    private static final List<String> AGENT_TABLES = List.of(
            "agent_task_defaults",
            "agent_task_schedules",
            "agent_task_queue",
            "agent_card_loadouts",
            "agent_cards",
            "agent_cms_users",
            "agent_cms_audit",
            "agent_chat_logs",
            "agent_economy_ledger",
            "agent_relationships",
            "agent_memory_events",
            "agent_action_logs",
            "agent_scripts",
            "agent_goals",
            "agent_runtime_sessions",
            "agent_policies",
            "agent_profiles"
    );

    private final JdbcTemplate agentJdbc;
    private final JdbcTemplate gameJdbc;
    private final String agentSchema;
    private final String gameSchema;

    public AgentGameSchemaInitializer(JdbcTemplate agentJdbc,
                                      @Qualifier("gameJdbc") JdbcTemplate gameJdbc,
                                      @Value("${spring.datasource.url}") String agentDatabaseUrl,
                                      @Value("${cosmic.game-database.url}") String gameDatabaseUrl) {
        this.agentJdbc = agentJdbc;
        this.gameJdbc = gameJdbc;
        this.agentSchema = schemaFromJdbcUrl(agentDatabaseUrl, "cosmic_agent_cms");
        this.gameSchema = schemaFromJdbcUrl(gameDatabaseUrl, "cosmic");
    }

    @PostConstruct
    public void ensureAgentCardTables() {
        validateSeparateSchemas();
        ensureFoundationTables();
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_cards (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    card_key VARCHAR(96) NOT NULL,
                    card_type VARCHAR(32) NOT NULL,
                    name VARCHAR(120) NOT NULL,
                    description VARCHAR(1000) NOT NULL,
                    priority INT NOT NULL DEFAULT 0,
                    enabled TINYINT NOT NULL DEFAULT 1,
                    built_in TINYINT NOT NULL DEFAULT 0,
                    config_json TEXT DEFAULT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_agent_cards_key (card_key),
                    KEY idx_agent_cards_type_enabled (card_type, enabled)
                )
                """);
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_card_loadouts (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    agent_profile_id INT NOT NULL,
                    slot_key VARCHAR(64) NOT NULL,
                    card_id BIGINT NOT NULL,
                    enabled TINYINT NOT NULL DEFAULT 1,
                    priority INT NOT NULL DEFAULT 0,
                    override_behavior TINYINT NOT NULL DEFAULT 0,
                    notes VARCHAR(500) DEFAULT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_agent_card_loadouts_slot (agent_profile_id, slot_key),
                    KEY idx_agent_card_loadouts_card (card_id),
                    KEY idx_agent_card_loadouts_profile_enabled (agent_profile_id, enabled)
                )
                """);
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_task_queue (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    agent_profile_id INT NOT NULL,
                    card_id BIGINT NOT NULL,
                    queue_order INT NOT NULL DEFAULT 0,
                    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                    run_mode VARCHAR(32) NOT NULL DEFAULT 'FINITE',
                    repeat_policy VARCHAR(32) NOT NULL DEFAULT 'ONCE',
                    parameter_json TEXT DEFAULT NULL,
                    starts_at TIMESTAMP NULL DEFAULT NULL,
                    expires_at TIMESTAMP NULL DEFAULT NULL,
                    completed_at TIMESTAMP NULL DEFAULT NULL,
                    locked_reason VARCHAR(255) DEFAULT NULL,
                    notes VARCHAR(500) DEFAULT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_agent_task_queue_profile_status_order (agent_profile_id, status, queue_order),
                    KEY idx_agent_task_queue_card (card_id)
                )
                """);
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_task_schedules (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    agent_profile_id INT NOT NULL,
                    card_id BIGINT NOT NULL,
                    enabled TINYINT NOT NULL DEFAULT 1,
                    schedule_name VARCHAR(120) NOT NULL DEFAULT 'Scheduled task',
                    days_of_week VARCHAR(32) NOT NULL DEFAULT 'ALL',
                    start_time TIME DEFAULT NULL,
                    end_time TIME DEFAULT NULL,
                    starts_at TIMESTAMP NULL DEFAULT NULL,
                    ends_at TIMESTAMP NULL DEFAULT NULL,
                    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Singapore',
                    priority INT NOT NULL DEFAULT 0,
                    run_mode VARCHAR(32) NOT NULL DEFAULT 'REUSABLE',
                    repeat_policy VARCHAR(32) NOT NULL DEFAULT 'LOOP',
                    parameter_json TEXT DEFAULT NULL,
                    notes VARCHAR(500) DEFAULT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_agent_task_schedules_profile_enabled_priority (agent_profile_id, enabled, priority),
                    KEY idx_agent_task_schedules_card (card_id)
                )
                """);
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_task_defaults (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    agent_profile_id INT NOT NULL,
                    card_id BIGINT NOT NULL,
                    enabled TINYINT NOT NULL DEFAULT 1,
                    priority INT NOT NULL DEFAULT 0,
                    selection_rule VARCHAR(32) NOT NULL DEFAULT 'PRIORITY',
                    run_mode VARCHAR(32) NOT NULL DEFAULT 'REUSABLE',
                    repeat_policy VARCHAR(32) NOT NULL DEFAULT 'LOOP',
                    parameter_json TEXT DEFAULT NULL,
                    notes VARCHAR(500) DEFAULT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_agent_task_defaults_card (agent_profile_id, card_id),
                    KEY idx_agent_task_defaults_profile_enabled_priority (agent_profile_id, enabled, priority)
                )
                """);
        ensureColumn("agent_task_queue", "parameter_json", "TEXT DEFAULT NULL AFTER repeat_policy");
        ensureColumn("agent_task_schedules", "parameter_json", "TEXT DEFAULT NULL AFTER repeat_policy");
        ensureColumn("agent_task_defaults", "parameter_json", "TEXT DEFAULT NULL AFTER repeat_policy");
        ensureColumn("agent_profiles", "deployment_channel", "INT NOT NULL DEFAULT 1 AFTER llm_enabled");
        ensureColumn("agent_profiles", "allowed_channels", "VARCHAR(128) NOT NULL DEFAULT '1' AFTER deployment_channel");
        migrateLegacyAgentTablesFromGameDatabase();
        seedBuiltInCards();
        seedExistingAgentLoadouts();
        dropLegacyAgentTablesFromGameDatabase();
    }

    private void ensureFoundationTables() {
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_profiles (
                    id INT NOT NULL AUTO_INCREMENT,
                    character_id INT NOT NULL,
                    ownership_type VARCHAR(16) NOT NULL DEFAULT 'SERVER',
                    owner_account_id INT DEFAULT NULL,
                    owner_character_id INT DEFAULT NULL,
                    enabled TINYINT NOT NULL DEFAULT 0,
                    display_name VARCHAR(32) DEFAULT NULL,
                    script_name VARCHAR(128) DEFAULT NULL,
                    llm_enabled TINYINT NOT NULL DEFAULT 0,
                    deployment_channel INT NOT NULL DEFAULT 1,
                    allowed_channels VARCHAR(128) NOT NULL DEFAULT '1',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_agent_profiles_character (character_id),
                    KEY idx_agent_profiles_owner_account (owner_account_id),
                    KEY idx_agent_profiles_owner_character (owner_character_id),
                    KEY idx_agent_profiles_enabled (enabled)
                )
                """);
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_runtime_sessions (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    agent_profile_id INT NOT NULL,
                    character_id INT NOT NULL,
                    world INT NOT NULL,
                    channel INT NOT NULL,
                    map_id INT NOT NULL,
                    state VARCHAR(32) NOT NULL DEFAULT 'LOADING',
                    current_goal_id BIGINT DEFAULT NULL,
                    current_task VARCHAR(128) DEFAULT NULL,
                    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    last_tick_at TIMESTAMP NULL DEFAULT NULL,
                    ended_at TIMESTAMP NULL DEFAULT NULL,
                    stop_reason VARCHAR(255) DEFAULT NULL,
                    PRIMARY KEY (id),
                    KEY idx_agent_sessions_profile_state (agent_profile_id, state),
                    KEY idx_agent_sessions_character_ended (character_id, ended_at),
                    KEY idx_agent_sessions_location (world, channel, map_id)
                )
                """);
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_goals (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    agent_profile_id INT NOT NULL,
                    goal_type VARCHAR(32) NOT NULL,
                    priority INT NOT NULL DEFAULT 0,
                    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                    target_world INT DEFAULT NULL,
                    target_channel INT DEFAULT NULL,
                    target_map INT DEFAULT NULL,
                    target_ref VARCHAR(128) DEFAULT NULL,
                    parameters_json TEXT DEFAULT NULL,
                    progress_json TEXT DEFAULT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    started_at TIMESTAMP NULL DEFAULT NULL,
                    completed_at TIMESTAMP NULL DEFAULT NULL,
                    PRIMARY KEY (id),
                    KEY idx_agent_goals_profile_status_priority (agent_profile_id, status, priority),
                    KEY idx_agent_goals_type_status (goal_type, status)
                )
                """);
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_scripts (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    name VARCHAR(128) NOT NULL,
                    version INT NOT NULL DEFAULT 1,
                    enabled TINYINT NOT NULL DEFAULT 1,
                    script_type VARCHAR(32) NOT NULL DEFAULT 'TEXT',
                    body MEDIUMTEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_agent_scripts_name (name),
                    KEY idx_agent_scripts_enabled (enabled)
                )
                """);
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_memory_events (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    agent_profile_id INT NOT NULL,
                    event_type VARCHAR(48) NOT NULL,
                    importance INT NOT NULL DEFAULT 0,
                    related_character_id INT DEFAULT NULL,
                    related_agent_profile_id INT DEFAULT NULL,
                    map_id INT DEFAULT NULL,
                    summary VARCHAR(500) NOT NULL,
                    details_json TEXT DEFAULT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_agent_memory_profile_time (agent_profile_id, created_at),
                    KEY idx_agent_memory_type_importance (event_type, importance)
                )
                """);
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_relationships (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    agent_profile_id INT NOT NULL,
                    related_character_id INT NOT NULL,
                    relationship_type VARCHAR(32) NOT NULL DEFAULT 'NEUTRAL',
                    trust_score INT NOT NULL DEFAULT 0,
                    affinity_score INT NOT NULL DEFAULT 0,
                    notes VARCHAR(500) DEFAULT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_agent_relationship_pair (agent_profile_id, related_character_id),
                    KEY idx_agent_relationship_related (related_character_id)
                )
                """);
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_action_logs (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    agent_profile_id INT NOT NULL,
                    runtime_session_id BIGINT DEFAULT NULL,
                    action_type VARCHAR(48) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    world INT DEFAULT NULL,
                    channel INT DEFAULT NULL,
                    map_id INT DEFAULT NULL,
                    target_type VARCHAR(48) DEFAULT NULL,
                    target_id BIGINT DEFAULT NULL,
                    message VARCHAR(512) DEFAULT NULL,
                    details_json TEXT DEFAULT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_agent_actions_profile_time (agent_profile_id, created_at),
                    KEY idx_agent_actions_session_time (runtime_session_id, created_at),
                    KEY idx_agent_actions_type_status (action_type, status)
                )
                """);
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_chat_logs (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    agent_profile_id INT NOT NULL,
                    runtime_session_id BIGINT DEFAULT NULL,
                    channel_type VARCHAR(32) NOT NULL DEFAULT 'MAP',
                    direction VARCHAR(16) NOT NULL DEFAULT 'OUT',
                    sender_character_id INT DEFAULT NULL,
                    recipient_character_id INT DEFAULT NULL,
                    message VARCHAR(500) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_agent_chat_profile_time (agent_profile_id, created_at),
                    KEY idx_agent_chat_sender_time (sender_character_id, created_at)
                )
                """);
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_economy_ledger (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    agent_profile_id INT NOT NULL,
                    runtime_session_id BIGINT DEFAULT NULL,
                    entry_type VARCHAR(32) NOT NULL,
                    item_id INT DEFAULT NULL,
                    quantity INT NOT NULL DEFAULT 0,
                    meso_delta BIGINT NOT NULL DEFAULT 0,
                    source_type VARCHAR(48) DEFAULT NULL,
                    source_id BIGINT DEFAULT NULL,
                    counterparty_character_id INT DEFAULT NULL,
                    world INT DEFAULT NULL,
                    channel INT DEFAULT NULL,
                    map_id INT DEFAULT NULL,
                    details_json TEXT DEFAULT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    KEY idx_agent_economy_profile_time (agent_profile_id, created_at),
                    KEY idx_agent_economy_item_time (item_id, created_at),
                    KEY idx_agent_economy_counterparty (counterparty_character_id)
                )
                """);
        agentJdbc.execute("""
                CREATE TABLE IF NOT EXISTS agent_policies (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    agent_profile_id INT NOT NULL DEFAULT 0,
                    policy_key VARCHAR(96) NOT NULL,
                    policy_value VARCHAR(500) NOT NULL,
                    notes VARCHAR(500) DEFAULT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_agent_policies_profile_key (agent_profile_id, policy_key)
                )
                """);
    }

    private void ensureColumn(String table, String column, String definition) {
        Integer exists = agentJdbc.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, table, column);
        if (exists != null && exists == 0) {
            agentJdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void migrateLegacyAgentTablesFromGameDatabase() {
        validateSeparateSchemas();
        boolean migrationSucceeded = true;
        for (String table : AGENT_TABLES) {
            if (!legacyTableExists(table)) {
                continue;
            }
            try {
                migrateLegacyTable(table);
            } catch (Exception exception) {
                migrationSucceeded = false;
                System.err.println("Unable to migrate legacy agent table " + table + ": " + exception.getMessage());
            }
        }
        if (!migrationSucceeded) {
            throw new IllegalStateException("Agent legacy table migration failed; refusing to drop legacy game-db agent tables");
        }
    }

    private void dropLegacyAgentTablesFromGameDatabase() {
        validateSeparateSchemas();
        List<String> tables = new ArrayList<>(AGENT_TABLES);
        Collections.reverse(tables);
        for (String table : tables) {
            if (legacyTableExists(table)) {
                gameJdbc.execute("DROP TABLE IF EXISTS " + quoteIdentifier(table));
            }
        }
    }

    private void validateSeparateSchemas() {
        if (agentSchema.equalsIgnoreCase(gameSchema)) {
            throw new IllegalStateException("Agent CMS database must be separate from the Cosmic game database. "
                    + "Resolved both datasources to '" + agentSchema + "'. Set AGENT_CMS_DB_URL to cosmic_agent_cms.");
        }
    }

    private boolean legacyTableExists(String table) {
        Integer exists = gameJdbc.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """, Integer.class, table);
        return exists != null && exists > 0;
    }

    private void migrateLegacyTable(String table) {
        List<String> sourceColumns = columns(gameJdbc, null, table);
        List<String> destinationColumns = columns(agentJdbc, null, table);
        Set<String> sourceColumnSet = new HashSet<>(sourceColumns);
        List<String> commonColumns = destinationColumns.stream()
                .filter(sourceColumnSet::contains)
                .toList();
        if (commonColumns.isEmpty()) {
            return;
        }

        String columnList = commonColumns.stream()
                .map(this::quoteIdentifier)
                .collect(Collectors.joining(", "));
        agentJdbc.update("INSERT IGNORE INTO " + quoteIdentifier(table)
                + " (" + columnList + ") SELECT " + columnList
                + " FROM " + quoteIdentifier(gameSchema) + "." + quoteIdentifier(table));
    }

    private List<String> columns(JdbcTemplate jdbc, String schema, String table) {
        String schemaPredicate = schema == null ? "DATABASE()" : "?";
        Object[] args = schema == null ? new Object[]{table} : new Object[]{schema, table};
        return jdbc.queryForList("""
                SELECT COLUMN_NAME
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = %s
                  AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
                """.formatted(schemaPredicate), String.class, args);
    }

    private String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private String schemaFromJdbcUrl(String jdbcUrl, String fallback) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return fallback;
        }
        int scheme = jdbcUrl.indexOf("://");
        int start = jdbcUrl.indexOf('/', scheme < 0 ? 0 : scheme + 3);
        if (start < 0 || start + 1 >= jdbcUrl.length()) {
            return fallback;
        }
        int end = jdbcUrl.indexOf('?', start + 1);
        String schema = (end < 0 ? jdbcUrl.substring(start + 1) : jdbcUrl.substring(start + 1, end)).trim();
        return schema.isBlank() ? fallback : schema;
    }

    private void seedBuiltInCards() {
        agentJdbc.update("""
                INSERT IGNORE INTO agent_cards(card_key, card_type, name, description, priority, enabled, built_in, config_json) VALUES
                    ('behavior.idle', 'BEHAVIOR', 'Idle', 'Wait calmly when no task, goal, or script is active.', 0, 1, 1, '{"intent":"IDLE"}'),
                    ('behavior.grinder', 'BEHAVIOR', 'Grinder', 'Prefer killing visible monsters and picking up nearby drops.', 20, 1, 1, '{"intent":"GRIND"}'),
                    ('behavior.looter', 'BEHAVIOR', 'Looter', 'Prefer nearby visible drop pickup before roaming for more drops.', 15, 1, 1, '{"intent":"LOOT"}'),
                    ('behavior.companion', 'BEHAVIOR', 'Companion', 'Stay near visible players and behave like a party companion.', 15, 1, 1, '{"intent":"FOLLOW_CHARACTER"}'),
                    ('behavior.town_idler', 'BEHAVIOR', 'Town idler', 'Hang around town-like social spaces and occasionally drift.', 10, 1, 1, '{"intent":"WAIT"}'),
                    ('behavior.roamer', 'BEHAVIOR', 'Roamer', 'Move around when no stronger task is active.', 10, 1, 1, '{"intent":"ROAM"}'),
                    ('behavior.movement_validator', 'BEHAVIOR', 'Movement validator', 'Cycle through local graph movement edges so operators can watch walk, step, jump, drop and climb animation in the v83 client.', 70, 1, 1, '{"intent":"ROAM","hint":"movement_validation","edgeTypes":["WALK","FOOTHOLD_LINK","JUMP","DROP","CLIMB"],"avoidPortals":true}'),
                    ('personality.default', 'PERSONALITY', 'Default', 'Neutral server-safe agent personality.', 0, 1, 1, '{"tone":"neutral"}'),
                    ('personality.friendly', 'PERSONALITY', 'Friendly', 'Warm, helpful, and more likely to greet players.', 10, 1, 1, '{"tone":"friendly"}'),
                    ('personality.quiet', 'PERSONALITY', 'Quiet', 'Keeps chatter low and focuses on assigned tasks.', 10, 1, 1, '{"tone":"quiet"}'),
                    ('personality.playful', 'PERSONALITY', 'Playful', 'Light and social without bypassing safety rules.', 10, 1, 1, '{"tone":"playful"}'),
                    ('personality.focused', 'PERSONALITY', 'Focused', 'Task-first personality for grinding or quest routines.', 10, 1, 1, '{"tone":"focused"}'),
                    ('task.idle', 'TASK', 'Idle until assigned', 'A no-op task card for agents that should do nothing unless goals are added.', 0, 1, 1, '{"behavior":"behavior.idle"}'),
                    ('task.chill_town', 'TASK', 'Chill in town', 'Hang around social town spaces when no explicit goal is active.', 10, 1, 1, '{"behavior":"behavior.town_idler"}'),
                    ('task.grind_to_level', 'TASK', 'Grind toward next level', 'Use safe grinding behavior as a placeholder for future level-target routines.', 20, 1, 1, '{"behavior":"behavior.grinder"}'),
                    ('task.follow_player', 'TASK', 'Follow a player', 'Companion-style placeholder task for future party/follow routines.', 20, 1, 1, '{"behavior":"behavior.companion"}'),
                    ('task.mapleisland_complete_all_quests', 'TASK', 'Complete Maple Island quests', 'Finish Maple Island quests in route order without taking one-way exits early, then end at Southperry.', 100, 1, 1, '{"behavior":"behavior.mapleisland_quester","runMode":"FINITE","repeatPolicy":"ONCE","region":"Maple Island","endpointMapId":60000,"endpointName":"Southperry","lockoutPolicy":"complete_map_quests_before_forward_only_transition","steps":["Amherst starter checks","Snail Hunting Ground training and ETC collection","Southperry Biggs and Shanks preparation","End idle at Southperry until assigned the next task"]}'),
                    ('task.hang_southperry', 'TASK', 'Hang around Southperry', 'Reusable town task that keeps the agent around Southperry and available for social behavior.', 45, 1, 1, '{"behavior":"behavior.town_social_hangout","runMode":"REUSABLE","repeatPolicy":"LOOP","targetMapId":60000,"targetName":"Southperry"}'),
                    ('task.hang_henesys', 'TASK', 'Hang around Henesys', 'Reusable town task that keeps the agent around Henesys for social roaming and light chatter.', 45, 1, 1, '{"behavior":"behavior.town_social_hangout","runMode":"REUSABLE","repeatPolicy":"LOOP","targetMapId":100000000,"targetName":"Henesys"}'),
                    ('task.hang_lith_harbor', 'TASK', 'Hang around Lith Harbor', 'Reusable town task that keeps the agent around Lith Harbor near new arrivals.', 45, 1, 1, '{"behavior":"behavior.town_social_hangout","runMode":"REUSABLE","repeatPolicy":"LOOP","targetMapId":104000000,"targetName":"Lith Harbor"}'),
                    ('task.grind_right_around_lith_harbor', 'TASK', 'Grind Right Around Lith Harbor', 'Reusable light grinding task for the first field after Lith Harbor.', 60, 1, 1, '{"behavior":"behavior.map_grinder","runMode":"REUSABLE","repeatPolicy":"LOOP","targetMapId":104000100,"targetName":"Right Around Lith Harbor","loot":true}'),
                    ('task.grind_henesys_hunting_ground_i', 'TASK', 'Grind Henesys Hunting Ground I', 'Reusable grinding task for the Henesys Hunting Ground I route near Henesys.', 60, 1, 1, '{"behavior":"behavior.map_grinder","runMode":"REUSABLE","repeatPolicy":"LOOP","targetMapId":104040000,"targetName":"Henesys Hunting Ground I","loot":true}'),
                    ('task.hang_kerning_subway', 'TASK', 'Hang around Kerning Subway', 'Reusable hangout task around the Kerning City Subway entrance.', 40, 1, 1, '{"behavior":"behavior.town_social_hangout","runMode":"REUSABLE","repeatPolicy":"LOOP","targetMapId":103000100,"targetName":"Subway Ticketing Booth"}'),
                    ('task.sit_on_relaxer', 'TASK', 'Sit on a Relaxer chair', 'Reusable calm task that prefers sitting on a Relaxer chair in a safe social map.', 35, 1, 1, '{"behavior":"behavior.chair_sitter","runMode":"REUSABLE","repeatPolicy":"LOOP","preferredItemId":3010000,"preferredItemName":"Relaxer"}'),
                    ('task.validate_movement', 'TASK', 'Validate movement animation', 'Reusable operator task that exercises walk, step, jump, drop and ladder or rope graph edges on the current map for live client validation.', 95, 1, 1, '{"behavior":"behavior.movement_validator","runMode":"REUSABLE","repeatPolicy":"LOOP","validation":"walk_step_jump_drop_climb","avoidPortals":true}'),
                    ('behavior.mapleisland_quester', 'BEHAVIOR', 'Maple Island quester', 'Quest-first behavior that avoids one-way transitions until local quest work is complete.', 60, 1, 1, '{"intent":"QUEST_ROUTE","avoidForwardOnlyLockout":true,"lootQuestItems":true}'),
                    ('behavior.town_social_hangout', 'BEHAVIOR', 'Town social hangout', 'Stay in a town or safe hub, drift occasionally, and remain available for social actions.', 35, 1, 1, '{"intent":"WAIT","socialDrift":true,"safeMapsOnly":true}'),
                    ('behavior.map_grinder', 'BEHAVIOR', 'Map grinder', 'Fight visible monsters on the assigned map and pick up nearby drops.', 45, 1, 1, '{"intent":"GRIND","stayOnAssignedMap":true,"lootNearby":true}'),
                    ('behavior.chair_sitter', 'BEHAVIOR', 'Chair sitter', 'Prefer sitting on a Relaxer or other configured chair while idle in a safe location.', 25, 1, 1, '{"intent":"WAIT","preferChair":true,"chairItemId":3010000}'),
                    ('personality.chatty_neighbor', 'PERSONALITY', 'Chatty neighbor', 'More likely to greet nearby players and agents with short friendly lines.', 30, 1, 1, '{"tone":"chatty","chatFrequency":"medium","allowAgentChat":true,"allowPlayerChat":true}'),
                    ('personality.relaxed_sitter', 'PERSONALITY', 'Relaxed sitter', 'Calm personality for agents that sit around town and talk occasionally.', 25, 1, 1, '{"tone":"relaxed","chatFrequency":"low","likesChairs":true}'),
                    ('personality.helpful_newcomer', 'PERSONALITY', 'Helpful newcomer', 'Friendly beginner-style personality that can explain simple places and directions.', 25, 1, 1, '{"tone":"helpful","chatFrequency":"medium","topicBias":["directions","quests","training"]}'),
                    ('personality.expressive_mood', 'PERSONALITY', 'Expressive mood', 'Uses fitting facial expressions like F3 or F7 and small playful movements when idle or social.', 30, 1, 1, '{"tone":"expressive","faces":["F1","F3","F7"],"motions":["jump","duck","playful_swing"],"frequency":"medium"}'),
                    ('policy.quest_lockout_safe', 'POLICY', 'Quest lockout safe', 'Constraint card for routes that must not leave one-way beginner areas before local quests are complete.', 50, 1, 1, '{"preventForwardOnlyExitUntilChecklistComplete":true}'),
                    ('policy.no_hidden_gm_awareness', 'POLICY', 'Ignore hidden staff', 'Constraint card documenting that hidden GM characters are not valid social, follow, or awareness targets.', 50, 1, 1, '{"ignoreHiddenStaff":true}'),
                    ('policy.safe_social_only', 'POLICY', 'Safe social only', 'Keeps passive social actions short, non-spammy, and server-safe.', 40, 1, 1, '{"chatCooldownSeconds":45,"noWhisperSpam":true,"noTradeSpam":true}'),
                    ('utility.expressive_idle_mix', 'UTILITY', 'Expressive idle mix', 'Adds occasional F3, F7, jump, duck, and playful swing actions while waiting in safe maps.', 35, 1, 1, '{"expressions":["F3","F7"],"motions":["JUMP","DUCK","SWING"],"safeMapsOnly":true,"cooldownSeconds":20}'),
                    ('utility.return_to_safe_town', 'UTILITY', 'Return to safe town', 'Utility preference for recovering to the nearest configured safe town when stuck or idle too long.', 30, 1, 1, '{"fallbackAction":"MOVE_TO_SAFE_TOWN","safeMapIds":[60000,104000000,100000000,103000000]}'),
                    ('utility.prefer_nearest_valid_task', 'UTILITY', 'Prefer nearest valid task', 'Default task selection hint that favors useful tasks near the current map.', 25, 1, 1, '{"selectionHint":"NEAREST","avoidLongTravelWhenIdle":true}'),
                    ('utility.relaxer_chair_idle', 'UTILITY', 'Relaxer chair idle', 'Utility preference to use a Relaxer chair when the active task allows waiting.', 20, 1, 1, '{"preferredChairItemId":3010000,"useWhenIdle":true}')
                """);
    }

    private void seedExistingAgentLoadouts() {
        agentJdbc.update("""
                INSERT IGNORE INTO agent_card_loadouts(agent_profile_id, slot_key, card_id, enabled, priority, notes)
                SELECT p.id, 'default_behavior_1', c.id, 1, 0, 'Default fallback equipped by Agent CMS startup initializer'
                FROM agent_profiles p
                JOIN agent_cards c ON c.card_key = 'behavior.idle'
                """);
        agentJdbc.update("""
                INSERT IGNORE INTO agent_card_loadouts(agent_profile_id, slot_key, card_id, enabled, priority, notes)
                SELECT p.id, 'personality_1', c.id, 1, 0, 'Default personality equipped by Agent CMS startup initializer'
                FROM agent_profiles p
                JOIN agent_cards c ON c.card_key = 'personality.default'
                """);
    }
}
