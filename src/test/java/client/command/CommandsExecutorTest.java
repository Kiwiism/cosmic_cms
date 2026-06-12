package client.command;

import org.junit.jupiter.api.Test;
import tools.Pair;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CommandsExecutorTest {
    private final CommandsExecutor commands = CommandsExecutor.getInstance();

    @Test
    void assignsRepresentativeCommandsToNewAccessLevels() {
        assertEquals(0, commands.getCommandRank("dispose"));
        assertEquals(1, commands.getCommandRank("gacha"));
        assertEquals(2, commands.getCommandRank("checkdmg"));
        assertEquals(2, commands.getCommandRank("resethp"));
        assertEquals(2, commands.getCommandRank("resetmp"));
        assertEquals(2, commands.getCommandRank("resethpmp"));
        assertEquals(3, commands.getCommandRank("gmshop"));
        assertEquals(4, commands.getCommandRank("giveitem"));
        assertEquals(5, commands.getCommandRank("servermessage"));
        assertEquals(6, commands.getCommandRank("shutdown"));
    }

    @Test
    void registersOnlyCanonicalCommandNames() {
        assertEquals(0, commands.getCommandRank("commands"));
        assertEquals(3, commands.getCommandRank("hide"));
        assertEquals(3, commands.getCommandRank("unhide"));
        assertEquals(-1, commands.getCommandRank("help"));
        assertEquals(-1, commands.getCommandRank("changel"));
        assertEquals(-1, commands.getCommandRank("dc"));
        assertEquals(-1, commands.getCommandRank("item"));
        assertEquals(-1, commands.getCommandRank("pe"));
        assertEquals(-1, commands.getCommandRank("togglehide"));
        assertEquals(-1, commands.getCommandRank("ap"));
        assertEquals(-1, commands.getCommandRank("sp"));
    }

    @Test
    void registersEveryLevelWithoutDuplicateImplementations() {
        List<Pair<List<String>, List<String>>> levels = commands.getGmCommands();

        assertEquals(7, levels.size());
        for (Pair<List<String>, List<String>> level : levels) {
            assertFalse(level.getLeft().isEmpty());
            assertEquals(level.getLeft().size(), level.getRight().size());
        }
        assertEquals(commands.getRegisteredCommandCount(), commands.getRegisteredCommandImplementationCount());
        assertEquals(
                commands.getRegisteredCommandCount(),
                levels.stream().mapToInt(level -> level.getLeft().size()).sum()
        );
    }
}
