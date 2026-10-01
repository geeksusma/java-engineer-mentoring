package java25.virtualthreads;

import java.time.Duration;

/**
 * Old-school code guarding its state with synchronized. Up to Java 23,
 * blocking inside a synchronized block "pinned" the virtual thread to its
 * carrier platform thread. Java 24 (JEP 491) removed that limitation.
 */
public class LegacyAccount {

    private long balance;

    public synchronized void slowDeposit(long amount, Duration latency) throws InterruptedException {
        Thread.sleep(latency);
        balance += amount;
    }

    public synchronized long balance() {
        return balance;
    }
}
