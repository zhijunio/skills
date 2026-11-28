import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

final class OrderObservation {

    private final ObservationRegistry registry;

    OrderObservation(ObservationRegistry registry) {
        this.registry = registry;
    }

    void createOrder(String channel, Runnable operation) {
        String boundedChannel = "web".equals(channel) ? "web"
            : "mobile".equals(channel) ? "mobile" : "other";
        Observation.createNotStarted("orders.create", registry)
            .lowCardinalityKeyValue("channel", boundedChannel)
            .observe(operation);
    }
}
