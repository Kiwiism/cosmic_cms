package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;
import server.quest.Quest;

public class PlayerCompleteQuestCommand extends Command {
    {
        setDescription("Force-complete a quest for a specified online player.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Targeted quest mutation is separated from the tester self-only command.
        if (params.length < 2) {
            client.getPlayer().yellowMessage("Syntax: !playercompletequest <playername> <questid>");
            return;
        }
        Character target = client.getWorldServer().getPlayerStorage().getCharacterByName(params[0]);
        Quest quest = Quest.getInstance(Integer.parseInt(params[1]));
        if (target == null || quest == null) {
            client.getPlayer().yellowMessage("Player or quest was not found.");
            return;
        }
        int npc = quest.getNpcRequirement(true);
        if (npc == -1) {
            target.getAbstractPlayerInteraction().forceCompleteQuest(quest.getId());
        } else {
            target.getAbstractPlayerInteraction().forceCompleteQuest(quest.getId(), npc);
        }
        client.getPlayer().message("Completed quest " + quest.getId() + " for " + target.getName() + ".");
    }
}
