package io.dndroll.backup;

import java.time.Duration;

/** Admission spans the entire synchronous request, including transaction completion. */
public final class WriteAdmission {
    public interface Lease extends AutoCloseable { @Override void close(); }
    private int active;
    private boolean paused;
    private boolean fenced;
    public synchronized Lease enter() {
        if (paused || fenced) throw new IllegalStateException("Writes paused");
        active++;
        return new Lease() {
            private boolean closed;
            public void close() {
                synchronized (WriteAdmission.this) {
                    if (!closed) { closed = true; active--; WriteAdmission.this.notifyAll(); }
                }
            }
        };
    }
    public synchronized Lease pause(Duration timeout) throws InterruptedException {
        if (paused || timeout.isNegative()) throw new IllegalStateException("Pause unavailable");
        paused = true;
        try {
            long remaining = timeout.toNanos(), start = System.nanoTime();
            while (active != 0) {
                if (Thread.interrupted()) throw new InterruptedException();
                if (remaining <= 0) throw new IllegalStateException("Drain timed out");
                java.util.concurrent.TimeUnit.NANOSECONDS.timedWait(this, remaining);
                remaining = timeout.toNanos() - (System.nanoTime() - start);
            }
        } catch (RuntimeException | InterruptedException failure) {
            paused = false; notifyAll(); throw failure;
        }
        return new Lease() {
            private boolean closed;
            public void close() {
                synchronized (WriteAdmission.this) {
                    if (!closed) { closed = true; paused = false; WriteAdmission.this.notifyAll(); }
                }
            }
        };
    }
    public synchronized void fence() { fenced = true; }
    public synchronized boolean paused() { return paused || fenced; }
}
