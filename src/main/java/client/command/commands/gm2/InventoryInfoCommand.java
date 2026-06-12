package client.command.commands.gm2;

import client.Client;
import client.command.Command;
import client.inventory.Inventory;
import client.inventory.InventoryType;

public class InventoryInfoCommand extends Command {
    {
        setDescription("Show used and available slots in your inventories.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Self-only tester diagnostic; it does not enumerate private item details.
        for (InventoryType type : InventoryType.values()) {
            if (type == InventoryType.UNDEFINED || type == InventoryType.EQUIPPED) {
                continue;
            }
            Inventory inventory = client.getPlayer().getInventory(type);
            client.getPlayer().message(type.name() + ": " + inventory.list().size()
                    + "/" + Byte.toUnsignedInt(inventory.getSlotLimit()) + " slots used.");
        }
    }
}
