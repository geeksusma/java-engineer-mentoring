package java25.virtualthreads;

/**
 * ScopedValue (final in Java 25, JEP 506) is the virtual-thread-friendly
 * replacement for ThreadLocal: immutable, bound for a bounded scope, and
 * automatically visible to subtasks forked inside that scope.
 */
public final class RequestContext {

    public static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

    private RequestContext() {
    }

    public static String handle(String requestId) {
        return ScopedValue.where(REQUEST_ID, requestId).call(RequestContext::process);
    }

    // Deep in the call stack, no parameter threading needed.
    static String process() {
        return "processed request " + REQUEST_ID.get();
    }

    public static String currentRequestIdOr(String fallback) {
        return REQUEST_ID.orElse(fallback);
    }
}
