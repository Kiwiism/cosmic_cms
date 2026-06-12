package client.command;

import client.Client;

/**
 * Base for intentionally registered command placeholders whose safe implementation
 * depends on configuration or persistence that is not part of the current server.
 */
public abstract class UnavailableCommand extends Command {
    private final String requirement;

    protected UnavailableCommand(String description, String requirement) {
        setDescription(description);
        this.requirement = requirement;
    }

    @Override
    public final void execute(Client client, String[] params) {
        client.getPlayer().yellowMessage("This command is not configured: " + requirement);
    }
}
