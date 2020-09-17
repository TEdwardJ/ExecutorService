package edu.ted.executor;

import java.util.concurrent.*;

public class SimpleExecutor implements Runnable {
    private final CountDownLatch terminationLatch;
    private final CyclicBarrier poolBarrier;
    private final String executorId;
    private volatile boolean inWork = true;

    private final BlockingQueue<FutureTask<?>> queue;

    public SimpleExecutor(BlockingQueue<FutureTask<?>> queue, String executorId, CyclicBarrier poolBarrier, CountDownLatch terminationLatch) {
        this.queue = queue;
        this.executorId = executorId;
        this.poolBarrier = poolBarrier;
        this.terminationLatch = terminationLatch;
    }

    public void run() {
        try {
            System.out.println(executorId + ": waiting for others");
            poolBarrier.await();
            System.out.println(executorId + ": started");

            while (inWork && !Thread.interrupted()) {
                FutureTask<?> task = getTask();
                System.out.println(executorId + ": got the task. Execution started");
                try {
                    task.run();
                } catch (Exception e) {
                    System.out.println(executorId + ": finished current task abnormally");
                    e.printStackTrace();
                }
            }
        } catch (BrokenBarrierException | InterruptedException e) {
            inWork = false;
        }
        terminationLatch.countDown();
        System.out.println(executorId + ": finished");
    }

    private FutureTask<?> getTask() throws InterruptedException {
        return queue.take();
    }

    void shutdown() {
        inWork = false;
    }
}
