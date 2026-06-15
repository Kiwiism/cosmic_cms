package server.runtime;

public interface RuntimeMetricsMBean {
    long getPacketsHandled();

    long getSlowPackets();

    long getScheduledTasksCompleted();

    long getSlowScheduledTasks();

    long getRejectedTasks();

    long getPersistenceQueued();

    long getPersistenceCompleted();

    long getPersistenceRejected();

    long getDatabaseConnectionsAcquired();

    long getDatabaseConnectionWaitMillis();

    long getCharacterSavesCompleted();

    long getCharacterSaveFailures();

    long getCharacterSaveMillis();

    long getMovementPackets();

    long getSlowMovementHandlers();

    long getSlowMovementGaps();

    long getSlowMovementVisibilityScans();

    long getMovementHandlerMillis();

    long getMovementVisibilityMillis();

    long getMaxMovementHandlerMillis();

    long getMaxMovementGapMillis();

    long getMaxMovementVisibilityMillis();

    long getMaxMovementVisibleObjects();

    long getMaxMovementRangeObjects();

    int getBackgroundQueueDepth();

    int getPersistenceQueueDepth();

    int getGameplaySchedulerQueueDepth();

    int getMaintenanceSchedulerQueueDepth();

    int getBackgroundActiveCount();

    int getPersistenceActiveCount();

    int getGameplaySchedulerActiveCount();

    int getMaintenanceSchedulerActiveCount();
}
