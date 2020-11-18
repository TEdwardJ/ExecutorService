package edu.ted.executorservice;


import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class AnyResultHolder<T> {

    private final Semaphore resultGiven = new Semaphore(1);
    private final AtomicReference<T> resultReference = new AtomicReference<>(null);
    private volatile boolean isResultSet;

    public AnyResultHolder() throws InterruptedException {
        resultGiven.acquire();
        log.debug("Semaphore acquired, permissions: {}", resultGiven.availablePermits());
    }

    public void setResult(T result) {
        if (resultReference.compareAndSet(null, result) && !isResultSet) {
            isResultSet = true;
            resultGiven.release();
            log.debug("Semaphore released, permissions: {}", resultGiven.availablePermits());
        }
    }

    public T getResult() throws InterruptedException {
        resultGiven.acquire();
        return resultReference.get();
    }

    public T getResult(long timeout, TimeUnit unit) throws InterruptedException {
        resultGiven.tryAcquire(timeout, unit);
        return resultReference.get();
    }

    public boolean isResultSet(){
        return isResultSet;
    }

}
