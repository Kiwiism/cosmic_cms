package server.configuration;

import config.YamlConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerCmsOverrideSwitchTest {
    private final boolean originalValue = YamlConfig.config.server.USE_SERVER_CMS_OVERRIDES;

    @AfterEach
    void restoreConfiguration() {
        YamlConfig.config.server.USE_SERVER_CMS_OVERRIDES = originalValue;
    }

    @Test
    void disabledSwitchUsesSourceCommandDefaultsWithoutDatabaseAccess() {
        YamlConfig.config.server.USE_SERVER_CMS_OVERRIDES = false;

        ServerConfigurationOverrides.applyStartupOverrides();
        CommandPolicyOverrides.load();

        assertTrue(CommandPolicyOverrides.enabled("commands"));
        assertTrue(CommandPolicyOverrides.enabled("nonexistent-command"));
    }
}
