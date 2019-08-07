package org.pb.android.geomap3d.util;

import java.util.concurrent.atomic.AtomicReference;

public abstract class AsyncRunnable<T> {

    private static final int WAIT_TIME_SINGLE_STEP_MS = 1500;
    private static final int WAIT_TIME_COMMON_STEPS = 5;

    protected abstract void run(AtomicReference<T> notifier);

    protected final void finish(AtomicReference<T> notifier, T result) {
        synchronized (notifier) {
            notifier.set(result);
            notifier.notify();
        }
    }

    public static <T> T wait(AsyncRunnable<T> runnable) {
        final AtomicReference<T> notifier = new AtomicReference<>();

        // run the asynchronous code
        runnable.run(notifier);

        // wait for the asynchronous code to finish
        synchronized (notifier) {
            while (notifier.get() == null) {
                try {
                    notifier.wait();
                } catch (InterruptedException ignore) {
                }
            }
        }

        // return the result of the asynchronous code
        return notifier.get();
    }

    // this is needed for the case if dao-call is interrupted from outside
    public static <T> T wait(AsyncRunnable<T> runnable, T errorResult) {
        final AtomicReference<T> notifier = new AtomicReference<>();
        int timeoutCounter = WAIT_TIME_COMMON_STEPS;

        // run the asynchronous code
        runnable.run(notifier);

        // wait for the asynchronous code to finish
        synchronized (notifier) {
            while (notifier.get() == null) {
                try {
                    notifier.wait(WAIT_TIME_SINGLE_STEP_MS);

                    if (timeoutCounter-- <= 0) {
                        notifier.set(errorResult);
                        notifier.notify();
                    }
                } catch (InterruptedException ignore) {
                    notifier.set(errorResult);
                    notifier.notify();
                }
            }
        }

        // return the result of the asynchronous code
        return notifier.get();
    }
}