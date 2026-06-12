package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;
import server.quest.Quest;

public class PlayerStartQuestCommand extends Command {
    {
        setDescription("Force-start a quest for a specified online player.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Targeted quest mutation is separated from the tester self-only command.
        if (params.length < 2) {
            client.getPlayer().yellowMessage("Syntax: !playerstartquest <playername> <questid>");
            return;
        }
        Character target = client.getWorldServer().getPlayerStorage().getCharacterByName(params[0]);
        Quest quest = Quest.getInstance(Integer.parseInt(params[1]));
        if (target == null || quest == null) {
            client.getPlayer().yellowMessage("Player or quest was not found.");
            return;
        }
        int npc = quest.getNpcRequirement(false);
        if (npc == -1) {
            target.getAbstractPlayerInteraction().forceStartQuest(quest.getId());
        } else {
            target.getAbstractPlayerInteraction().forceStartQuest(quest.getId(), npc);
        }
        client.getPlayer().message("Started quest " + quest.getId() + " for " + target.getName() + ".");
    }
}
