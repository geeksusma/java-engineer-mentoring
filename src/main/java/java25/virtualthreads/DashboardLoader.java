package java25.virtualthreads;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

/**
 * StructuredTaskScope is still a preview API in Java 25 (JEP 505).
 */
public final class DashboardLoader {

    private DashboardLoader() {
    }

    public static Dashboard load(Callable<String> fetchUser, Callable<List<String>> fetchOrders)
            throws InterruptedException {
        // open() uses the "all must succeed" policy: the first failing
        // subtask cancels the others and join() throws FailedException.
        try (var scope = StructuredTaskScope.open()) {
            Subtask<String> user = scope.fork(fetchUser);
            Subtask<List<String>> orders = scope.fork(fetchOrders);

            scope.join();

            return new Dashboard(user.get(), orders.get());
        }
    }
}
