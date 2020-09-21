package edu.ted.executorservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class SimpleWorker implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Thread myThread;
    private final Semaphore isRunningSemaphore = new Semaphore(1, false);
    private final CountDownLatch terminationLatch;
    private final String workerId;
    private volatile boolean isRunning = true;

    private final SimpleLinkedBlockingQueue<FutureTask<?>> queue;

    public SimpleWorker(SimpleLinkedBlockingQueue<FutureTask<?>> queue, String workerId, CountDownLatch terminationLatch) {
        this.queue = queue;
        this.workerId = workerId;
        this.terminationLatch = terminationLatch;
    }

    public void run() {
        try {
            myThread = Thread.currentThread();
            logger.debug("{}: started in {}", workerId, myThread.getName());

            while (isRunning && !Thread.interrupted()) {
                FutureTask<?> task = getTask();
                isRunningSemaphore.acquire();
                try {
                    logger.debug("{}: got the task. Execution started", workerId);
                    task.run();
                } catch (Exception e) {
                    logger.debug("{}: finished current task abnormally", workerId, e);
                } finally {
                    isRunningSemaphore.release();
                }
            }
        } catch (InterruptedException e) {
            logger.debug("{}: worker was interrupted", workerId, e);
            shutdown();
        } finally {
            terminationLatch.countDown();
            logger.debug("{}: finished", workerId);
        }
    }

    private FutureTask<?> getTask() throws InterruptedException {
        return queue.take();
    }

    void shutdown() {
        isRunning = false;
    }

    boolean isIdle() {
        try {
            boolean freeMarker = isRunningSemaphore.tryAcquire();
            if (freeMarker) {
                return true;
            }
        } finally {
            isRunningSemaphore.release();
        }
        return false;
    }

    Thread getWorkerThread() {
        return myThread;
    }

}
