package server.runtime;

import net.server.Server;

public final class RuntimeModuleContext {
    private final Server server;
    private final RuntimeEventBus eventBus;

    RuntimeModuleContext(Server server, RuntimeEventBus eventBus) {
        this.server = server;
        this.eventBus = eventBus;
    }

    public Server server() {
        return server;
    }

    public RuntimeEventBus eventBus() {
        return eventBus;
    }
}
