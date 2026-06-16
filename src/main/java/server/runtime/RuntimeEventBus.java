package server.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class RuntimeEventBus {
    private static final Logger log = LoggerFactory.getLogger(RuntimeEventBus.class);

    private final Map<Class<? extends RuntimeEvent>, CopyOnWriteArrayList<Consumer<? extends RuntimeEvent>>> listeners =
            new ConcurrentHashMap<>();

    public <T extends RuntimeEvent> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void publish(RuntimeEvent event) {
        if (event == null) {
            return;
        }

        dispatch(event, event.getClass());
        dispatch(event, RuntimeEvent.class);
    }

    @SuppressWarnings("unchecked")
    private <T extends RuntimeEvent> void dispatch(RuntimeEvent event, Class<T> eventType) {
        List<Consumer<? extends RuntimeEvent>> eventListeners = listeners.get(eventType);
        if (eventListeners == null || eventListeners.isEmpty()) {
            return;
        }

        for (Consumer<? extends RuntimeEvent> listener : eventListeners) {
            try {
                ((Consumer<T>) listener).accept((T) event);
            } catch (Exception e) {
                log.warn("Runtime event listener failed for {}", eventType.getSimpleName(), e);
            }
        }
    }

    public void clear() {
        listeners.clear();
    }
}
