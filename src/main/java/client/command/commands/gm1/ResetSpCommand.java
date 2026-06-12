package client.command.commands.gm1;

import client.Character;
import client.Client;
import client.command.Command;

public class ResetSpCommand extends Command {
    {
        setDescription("Reset current job-tree skills and refund their SP.");
    }

    @Override
    public void execute(Client client, String[] params) {
        Character player = client.getPlayer();
        int availableSp = player.resetSkillPoints();
        player.yellowMessage("Job skills reset. Available SP restored to the legitimate total of " + availableSp + ".");
    }
}
