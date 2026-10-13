package macrodesign.boundedcontext.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * In-memory stand-in for a message broker (Kafka, RabbitMQ...). Contexts
 * integrate by publishing and subscribing, never by calling each other's classes.
 */
public final class EventBus {

    private record Subscription<T>(Class<T> type, Consumer<? super T> handler) {

        void deliver(Object event) {
            if (type.isInstance(event)) {
                handler.accept(type.cast(event));
            }
        }
    }

    private final List<Subscription<?>> subscriptions = new ArrayList<>();

    public <T> void subscribe(Class<T> type, Consumer<? super T> handler) {
        subscriptions.add(new Subscription<>(type, handler));
    }

    public void publish(Object event) {
        subscriptions.forEach(subscription -> subscription.deliver(event));
    }
}
