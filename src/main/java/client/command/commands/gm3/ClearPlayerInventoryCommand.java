package client.command.commands.gm3;

import client.Character;
import client.Client;
import client.command.Command;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.manipulator.InventoryManipulator;

import java.util.ArrayList;

public class ClearPlayerInventoryCommand extends Command {
    {
        setDescription("Clear one inventory tab for a specified online player.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Destructive targeted support action; an explicit player and tab are mandatory.
        if (params.length < 2) {
            client.getPlayer().yellowMessage("Syntax: !clearplayerinventory <playername> <equip|use|setup|etc|cash>");
            return;
        }
        Character target = client.getWorldServer().getPlayerStorage().getCharacterByName(params[0]);
        if (target == null) {
            client.getPlayer().message("Player '" + params[0] + "' was not found in this world.");
            return;
        }

        InventoryType type;
        try {
            type = InventoryType.valueOf(params[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            client.getPlayer().yellowMessage("Inventory tab must be equip, use, setup, etc, or cash.");
            return;
        }
        if (type == InventoryType.UNDEFINED || type == InventoryType.EQUIPPED) {
            client.getPlayer().yellowMessage("That inventory tab cannot be cleared with this command.");
            return;
        }

        Inventory inventory = target.getInventory(type);
        for (Item item : new ArrayList<>(inventory.list())) {
            InventoryManipulator.removeFromSlot(target.getClient(), type, item.getPosition(),
                    item.getQuantity(), false, false);
        }
        client.getPlayer().message("Cleared " + target.getName() + "'s " + type.name() + " inventory.");
    }
}
