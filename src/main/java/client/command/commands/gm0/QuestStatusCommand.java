package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import server.quest.Quest;

public class QuestStatusCommand extends Command {
    {
        setDescription("Show your status for a quest ID.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Read-only lookup against the invoking character.
        Character player = client.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: @queststatus <questid>");
            return;
        }

        int questId;
        try {
            questId = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            player.yellowMessage("Quest ID must be a number.");
            return;
        }

        Quest quest = Quest.getInstance(questId);
        if (quest == null) {
            player.yellowMessage("Quest " + questId + " does not exist.");
            return;
        }

        String status = switch (player.getQuestStatus(questId)) {
            case 1 -> "started";
            case 2 -> "completed";
            default -> "not started";
        };
        player.message("Quest " + questId + " (" + quest.getName() + "): " + status + ".");
    }
}
