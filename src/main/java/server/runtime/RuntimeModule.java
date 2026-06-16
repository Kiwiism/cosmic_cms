package server.runtime;

/**
 * Small lifecycle boundary for optional runtime systems.
 *
 * Modules should be conservative: startup must be fast, shutdown must be idempotent,
 * and failures should stay contained so optional systems do not destabilize the core server.
 */
public interface RuntimeModule {
    String name();

    default void start(RuntimeModuleContext context) throws Exception {
    }

    default void stop(RuntimeModuleContext context) throws Exception {
    }
}
