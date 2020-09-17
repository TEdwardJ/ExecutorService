package edu.ted.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class CallableTaskWrapper<T> implements Callable<T> {

    private final Callable<T> wrappedCallable;
    private final CountDownLatch timedLatch;

    public CallableTaskWrapper(Callable<T> wrappedCallable, CountDownLatch timedLatch) {
        this.wrappedCallable = wrappedCallable;
        this.timedLatch = timedLatch;
    }

    @Override
    public T call() throws Exception {
        T result = wrappedCallable.call();
        System.out.println("Latch Count: " + timedLatch.getCount());
        timedLatch.countDown();
        return result;
    }
}
