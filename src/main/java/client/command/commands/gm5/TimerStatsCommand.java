package client.command.commands.gm5;

import client.Client;
import client.command.Command;
import server.TimerManager;

public class TimerStatsCommand extends Command {
    {
        setDescription("Show scheduled timer executor statistics.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Read-only scheduler metrics already exposed by TimerManager's MBean.
        TimerManager timer = TimerManager.getInstance();
        client.getPlayer().yellowMessage("Timer manager");
        client.getPlayer().message("Active: " + timer.getActiveCount() + ", queued: "
                + timer.getQueuedTasks() + ", completed: " + timer.getCompletedTaskCount()
                + ", submitted: " + timer.getTaskCount());
    }
}
