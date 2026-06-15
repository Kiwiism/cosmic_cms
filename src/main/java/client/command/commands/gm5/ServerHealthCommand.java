package client.command.commands.gm5;

import client.Client;
import client.command.Command;
import net.server.Server;
import net.server.channel.Channel;
import server.runtime.RuntimeMetrics;

public class ServerHealthCommand extends Command {
    {
        setDescription("Show JVM memory and online population health.");
    }

    @Override
    public void execute(Client client, String[] params) {
        // Read-only operational snapshot; detailed session/IP data remains separate.
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long committedMb = runtime.totalMemory() / 1024 / 1024;
        long maxMb = runtime.maxMemory() / 1024 / 1024;

        int online = 0;
        for (Channel channel : Server.getInstance().getChannelsFromWorld(client.getWorld())) {
            online += channel.getPlayerStorage().getSize();
        }

        client.getPlayer().yellowMessage("Server health");
        client.getPlayer().message("Memory: " + usedMb + " MB used / " + committedMb
                + " MB committed / " + maxMb + " MB max.");
        client.getPlayer().message("World players online: " + online
                + ", JVM threads: " + Thread.activeCount());
        RuntimeMetrics metrics = RuntimeMetrics.getInstance();
        client.getPlayer().message("Packets: " + metrics.getPacketsHandled()
                + " handled, " + metrics.getSlowPackets() + " slow. Movement: "
                + metrics.getMovementPackets() + " packets, max handler "
                + metrics.getMaxMovementHandlerMillis() + " ms, max gap "
                + metrics.getMaxMovementGapMillis() + " ms.");
        client.getPlayer().message("Queues: gameplay " + metrics.getGameplaySchedulerQueueDepth()
                + ", maintenance " + metrics.getMaintenanceSchedulerQueueDepth()
                + ", background " + metrics.getBackgroundQueueDepth()
                + ", persistence " + metrics.getPersistenceQueueDepth() + ".");
        client.getPlayer().message("Persistence: " + metrics.getCharacterSavesCompleted()
                + " saves, " + metrics.getCharacterSaveFailures() + " failures. DB wait total "
                + metrics.getDatabaseConnectionWaitMillis() + " ms.");
    }
}
