package client.inventory;

import org.junit.jupiter.api.Test;
import testutil.Mocks;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventoryTest {

    @Test
    void listReturnsStableSnapshot() {
        Inventory inventory = new Inventory(Mocks.chr(), InventoryType.USE, (byte) 24);
        inventory.addItem(new Item(2000000, (short) 0, (short) 1));

        Collection<Item> snapshot = inventory.list();
        inventory.addItem(new Item(2000001, (short) 0, (short) 1));

        assertEquals(1, snapshot.size());
        assertEquals(2, inventory.list().size());
    }

    @Test
    void removeItemIgnoresNonPositiveQuantity() {
        Inventory inventory = new Inventory(Mocks.chr(), InventoryType.USE, (byte) 24);
        short slot = inventory.addItem(new Item(2000000, (short) 0, (short) 3));

        inventory.removeItem(slot, (short) 0, false);
        inventory.removeItem(slot, (short) -1, false);

        assertNotNull(inventory.getItem(slot));
        assertEquals(3, inventory.getItem(slot).getQuantity());
    }

    @Test
    void removeItemClampsAndRemovesEmptyStack() {
        Inventory inventory = new Inventory(Mocks.chr(), InventoryType.USE, (byte) 24);
        short slot = inventory.addItem(new Item(2000000, (short) 0, (short) 3));

        inventory.removeItem(slot, (short) 10, false);

        assertNull(inventory.getItem(slot));
    }
}
