package server.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.IntSupplier;

public final class RuntimeMetrics implements RuntimeMetricsMBean {
    private static final Logger log = LoggerFactory.getLogger(RuntimeMetrics.class);
    private static final RuntimeMetrics instance = new RuntimeMetrics();

    private final LongAdder packetsHandled = new LongAdder();
    private final LongAdder slowPackets = new LongAdder();
    private final LongAdder scheduledTasksCompleted = new LongAdder();
    private final LongAdder slowScheduledTasks = new LongAdder();
    private final LongAdder rejectedTasks = new LongAdder();
    private final LongAdder persistenceQueued = new LongAdder();
    private final LongAdder persistenceCompleted = new LongAdder();
    private final LongAdder persistenceRejected = new LongAdder();
    private final LongAdder databaseConnectionsAcquired = new LongAdder();
    private final LongAdder databaseConnectionWaitMillis = new LongAdder();
    private final LongAdder characterSavesCompleted = new LongAdder();
    private final LongAdder characterSaveFailures = new LongAdder();
    private final LongAdder characterSaveMillis = new LongAdder();

    private volatile IntSupplier backgroundQueueDepth = () -> 0;
    private volatile IntSupplier persistenceQueueDepth = () -> 0;
    private volatile IntSupplier gameplaySchedulerQueueDepth = () -> 0;
    private volatile IntSupplier maintenanceSchedulerQueueDepth = () -> 0;
    private volatile IntSupplier backgroundActiveCount = () -> 0;
    private volatile IntSupplier persistenceActiveCount = () -> 0;
    private volatile IntSupplier gameplaySchedulerActiveCount = () -> 0;
    private volatile IntSupplier maintenanceSchedulerActiveCount = () -> 0;

    private RuntimeMetrics() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("cosmic:type=RuntimeMetrics");
            if (!server.isRegistered(name)) {
                server.registerMBean(this, name);
            }
        } catch (Exception e) {
            log.warn("Unable to register runtime metrics MBean", e);
        }
    }

    public static RuntimeMetrics getInstance() {
        return instance;
    }

    public void recordPacket(long elapsedMillis, long slowThresholdMillis) {
        packetsHandled.increment();
        if (elapsedMillis >= slowThresholdMillis) {
            slowPackets.increment();
        }
    }

    public void recordScheduledTask(long elapsedMillis, long slowThresholdMillis) {
        scheduledTasksCompleted.increment();
        if (elapsedMillis >= slowThresholdMillis) {
            slowScheduledTasks.increment();
        }
    }

    public void recordRejectedTask() {
        rejectedTasks.increment();
    }

    public void recordPersistenceQueued() {
        persistenceQueued.increment();
    }

    public void recordPersistenceCompleted() {
        persistenceCompleted.increment();
    }

    public void recordPersistenceRejected() {
        persistenceRejected.increment();
    }

    public void recordDatabaseConnection(long elapsedMillis) {
        databaseConnectionsAcquired.increment();
        databaseConnectionWaitMillis.add(elapsedMillis);
    }

    public void recordCharacterSave(long elapsedMillis, boolean success) {
        characterSaveMillis.add(elapsedMillis);
        if (success) {
            characterSavesCompleted.increment();
        } else {
            characterSaveFailures.increment();
        }
    }

    public void bindQueueDepths(IntSupplier background, IntSupplier persistence,
                                IntSupplier gameplayScheduler, IntSupplier maintenanceScheduler) {
        backgroundQueueDepth = background;
        persistenceQueueDepth = persistence;
        gameplaySchedulerQueueDepth = gameplayScheduler;
        maintenanceSchedulerQueueDepth = maintenanceScheduler;
    }

    public void bindActiveCounts(IntSupplier background, IntSupplier persistence,
                                 IntSupplier gameplayScheduler, IntSupplier maintenanceScheduler) {
        backgroundActiveCount = background;
        persistenceActiveCount = persistence;
        gameplaySchedulerActiveCount = gameplayScheduler;
        maintenanceSchedulerActiveCount = maintenanceScheduler;
    }

    public String healthSnapshot() {
        return "packets=" + packetsHandled.sum()
                + ", slowPackets=" + slowPackets.sum()
                + ", schedulerQueue=" + getGameplaySchedulerQueueDepth()
                + ", maintenanceQueue=" + getMaintenanceSchedulerQueueDepth()
                + ", backgroundQueue=" + getBackgroundQueueDepth()
                + ", persistenceQueue=" + getPersistenceQueueDepth()
                + ", activeWorkers={background=" + getBackgroundActiveCount()
                + ", persistence=" + getPersistenceActiveCount()
                + ", gameplay=" + getGameplaySchedulerActiveCount()
                + ", maintenance=" + getMaintenanceSchedulerActiveCount() + "}"
                + ", saveFailures=" + characterSaveFailures.sum()
                + ", rejectedTasks=" + rejectedTasks.sum();
    }

    @Override
    public long getPacketsHandled() {
        return packetsHandled.sum();
    }

    @Override
    public long getSlowPackets() {
        return slowPackets.sum();
    }

    @Override
    public long getScheduledTasksCompleted() {
        return scheduledTasksCompleted.sum();
    }

    @Override
    public long getSlowScheduledTasks() {
        return slowScheduledTasks.sum();
    }

    @Override
    public long getRejectedTasks() {
        return rejectedTasks.sum();
    }

    @Override
    public long getPersistenceQueued() {
        return persistenceQueued.sum();
    }

    @Override
    public long getPersistenceCompleted() {
        return persistenceCompleted.sum();
    }

    @Override
    public long getPersistenceRejected() {
        return persistenceRejected.sum();
    }

    @Override
    public long getDatabaseConnectionsAcquired() {
        return databaseConnectionsAcquired.sum();
    }

    @Override
    public long getDatabaseConnectionWaitMillis() {
        return databaseConnectionWaitMillis.sum();
    }

    @Override
    public long getCharacterSavesCompleted() {
        return characterSavesCompleted.sum();
    }

    @Override
    public long getCharacterSaveFailures() {
        return characterSaveFailures.sum();
    }

    @Override
    public long getCharacterSaveMillis() {
        return characterSaveMillis.sum();
    }

    @Override
    public int getBackgroundQueueDepth() {
        return backgroundQueueDepth.getAsInt();
    }

    @Override
    public int getPersistenceQueueDepth() {
        return persistenceQueueDepth.getAsInt();
    }

    @Override
    public int getGameplaySchedulerQueueDepth() {
        return gameplaySchedulerQueueDepth.getAsInt();
    }

    @Override
    public int getMaintenanceSchedulerQueueDepth() {
        return maintenanceSchedulerQueueDepth.getAsInt();
    }

    @Override
    public int getBackgroundActiveCount() {
        return backgroundActiveCount.getAsInt();
    }

    @Override
    public int getPersistenceActiveCount() {
        return persistenceActiveCount.getAsInt();
    }

    @Override
    public int getGameplaySchedulerActiveCount() {
        return gameplaySchedulerActiveCount.getAsInt();
    }

    @Override
    public int getMaintenanceSchedulerActiveCount() {
        return maintenanceSchedulerActiveCount.getAsInt();
    }
}
