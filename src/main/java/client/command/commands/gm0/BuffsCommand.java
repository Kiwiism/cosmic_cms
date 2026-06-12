package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import net.server.PlayerBuffValueHolder;

import java.util.List;

public class BuffsCommand extends Command {
    {
        setDescription("List your active buff sources.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Read-only snapshot; source IDs are useful for player support and bug reports.
        List<PlayerBuffValueHolder> buffs = client.getPlayer().getAllBuffs();
        if (buffs.isEmpty()) {
            client.getPlayer().message("You have no active buffs.");
            return;
        }

        client.getPlayer().yellowMessage("Active buffs: " + buffs.size());
        for (PlayerBuffValueHolder buff : buffs) {
            client.getPlayer().message("Source " + buff.effect.getBuffSourceId());
        }
    }
}
