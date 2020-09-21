package edu.ted.executorservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class SimpleFixedThreadPool implements ExecutorService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final int poolSize;
    private final AtomicInteger workersCount = new AtomicInteger(0);
    private final Semaphore shutdownSemaphore = new Semaphore(1);
    private final CountDownLatch terminationLatch;
    private final SimpleBlockingQueue<FutureTask<?>> queue = new SimpleBlockingQueue<>(100);
    private final List<SimpleWorker> workerList;
    private volatile boolean isRunning;

    public SimpleFixedThreadPool(int poolSize) {
        this.poolSize = poolSize;
        terminationLatch = new CountDownLatch(poolSize);
        workerList = Collections.synchronizedList(new ArrayList<>(poolSize));
        isRunning = true;
    }

    public void shutdown() {
        if (!shutdownSemaphore.tryAcquire()) {
            return;
        }
        softShutdownThreads();
        interruptThreadsByFilter(SimpleWorker::isIdle);
    }

    public List<Runnable> shutdownNow() {
        List<Runnable> restOfTasksList = new ArrayList<>();
        if (!shutdownSemaphore.tryAcquire()) {
            return restOfTasksList;
        }
        softShutdownThreads();
        queue.drainTo(restOfTasksList);
        interruptThreadsByFilter(w -> true);
        return restOfTasksList;
    }

    public boolean isShutdown() {
        return !isRunning;
    }

    public boolean isTerminated() {
        return terminationLatch.getCount() == 0;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return terminationLatch.await(timeout, unit);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return internalSubmit(new FutureTask<>(task));
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return internalSubmit(new FutureTask<>(task, result));
    }

    public Future<?> submit(Runnable task) {
        return internalSubmit(new FutureTask<>(task, true));
    }

    public void execute(Runnable command) {
        submit(command);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        return new ArrayList<>();
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
        return new ArrayList<>();
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    private void checkForRejection() {
        if (!isRunning) {
            throw new RejectedExecutionException();
        }
    }

    private void interruptThreadsByFilter(Predicate<SimpleWorker> filterCondition) {
        for (SimpleWorker worker : workerList) {
            if (filterCondition.test(worker)) {
                worker.getWorkerThread().interrupt();
            }
        }
    }

    private void softShutdownThreads() {
        if (!isRunning) {
            return;
        }
        isRunning = false;
        for (SimpleWorker simpleWorker : workerList) {
            simpleWorker.shutdown();
        }
    }

    private void startNewWorkerIfNeeded() {
        boolean workerSuccessfullyAddedFlag = false;
        int currentWorkersCount;
        while (!workerSuccessfullyAddedFlag && (currentWorkersCount = workersCount.get()) < poolSize) {
            int currentIndex = currentWorkersCount - 1;
            SimpleWorker worker = new SimpleWorker(queue, "Executor#" + currentIndex, terminationLatch);
            //Critical point: if 2 or more competitors are to add new worker  - only one will win, second will go on second round
            //in case of second round the work done above is overhead;
            if (workersCount.compareAndSet(currentWorkersCount, (currentWorkersCount + 1))) {
                workerSuccessfullyAddedFlag = true;
                workerList.add(worker);
                Thread workerThread = new Thread(worker);
                workerThread.setName("Worker " + currentIndex + " thread");
                workerThread.start();
                logger.debug("New Worker {} added", currentIndex);
            }
        }
    }

    private <T> Future<T> internalSubmit(FutureTask<T> futureTask) {
        checkForRejection();
        startNewWorkerIfNeeded();
        queue.put(futureTask);
        return futureTask;
    }

}
