package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;

public class CharInfoCommand extends Command {
    {
        setDescription("Show a self-only testing summary of your character.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Self-only tester insight; no account, network, or hardware identifiers.
        Character player = client.getPlayer();
        player.yellowMessage(player.getName() + " - level " + player.getLevel() + " " + player.getJob());
        player.message("HP/MP: " + player.getHp() + "/" + player.getCurrentMaxHp()
                + " | " + player.getMp() + "/" + player.getCurrentMaxMp());
        player.message("AP/SP: " + player.getRemainingAp() + "/" + player.getRemainingSp()
                + ", mesos: " + player.getMeso());
        player.message("EXP/Meso/Drop/Boss: " + player.getExpRate() + "x/"
                + player.getMesoRate() + "x/" + player.getDropRate() + "x/"
                + player.getBossDropRate() + "x");
    }
}
