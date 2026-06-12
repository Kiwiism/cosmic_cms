package client.command.commands.gm3;

import client.command.UnavailableCommand;

public class StaffNoteCommand extends UnavailableCommand {
    public StaffNoteCommand() {
        // External dependency: moderation notes need durable authorship and timestamps.
        super("Add a persistent moderation note for a player.",
                "add a staff_notes database table with actor, target, note, and timestamp columns");
    }
}
