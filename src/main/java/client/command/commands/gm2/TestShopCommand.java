package client.command.commands.gm2;

import client.command.UnavailableCommand;

public class TestShopCommand extends UnavailableCommand {
    public TestShopCommand() {
        // External dependency: shops and shop items are stored in the database.
        super("Open a restricted sandbox testing shop.",
                "create the tester shop in the shops/shopitems tables and add an allowlisted shop ID to server configuration");
    }
}
