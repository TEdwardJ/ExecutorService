package edu.ted.executorservice;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Slf4j
public class SimpleFixedThreadPool implements ExecutorService {

    private final int poolSize;
    private final AtomicInteger workersCount = new AtomicInteger(0);
    private final Semaphore shutdownSemaphore = new Semaphore(1);
    private final CountDownLatch terminationLatch;
    private final SimpleLinkedBlockingQueue<FutureTask<?>> queue = new SimpleLinkedBlockingQueue<>();
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

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        CountDownLatch timedLatch = new CountDownLatch(tasks.size());
        List<Semaphore> semaphoreList = new ArrayList<>();
        List<Future<T>> futureList = invokeAllAsync(tasks, timedLatch, semaphoreList);
        timedLatch.await();
        for (int i = 0; i < semaphoreList.size(); i++) {
            if (semaphoreList.get(i).tryAcquire()) {
                try {
                    T result = futureList.get(i).get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
        return futureList;
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        final CountDownLatch timedLatch = new CountDownLatch(tasks.size());
        List<Semaphore> semaphoreList = new ArrayList<>();
        List<Future<T>> futureList = invokeAllAsync(tasks, timedLatch, semaphoreList);
        timedLatch.await(timeout, unit);
        for (int i = 0; i < semaphoreList.size(); i++) {
            if (!semaphoreList.get(i).tryAcquire()) {
                futureList.get(i).cancel(true);
            } else{
                try {
                    T result = futureList.get(i).get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
        return futureList;
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        AnyResultHolder<T> resultHolder = new AnyResultHolder<>();
        List<Future<T>> futureList = invokeAnyAsync(tasks, resultHolder);
        T someResult = resultHolder.getResult();
        try {
            CheckAndCompleteFuture(futureList, false, resultHolder.isResultSet());
        } catch (TimeoutException e) {
            //should not happen if throwTimePutException == false
        }
        return someResult;
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        AnyResultHolder<T> resultHolder = new AnyResultHolder<>();
        List<Future<T>> futureList = invokeAnyAsync(tasks, resultHolder);
        T someResult = resultHolder.getResult(timeout, unit);
        log.debug("Result is taken: {}", someResult);
        CheckAndCompleteFuture(futureList, true, resultHolder.isResultSet());
        return someResult;
    }

    private <T> void CheckAndCompleteFuture(List<Future<T>> futureList, boolean throwTimeOutException, boolean isResultSet) throws TimeoutException, ExecutionException {
        int successfullyCompletedTaskNumber, failedTaskNumber, inProgressTaskNumber;
        successfullyCompletedTaskNumber = failedTaskNumber = inProgressTaskNumber = 0;
        for (Future<T> future : futureList) {
            if (future.isDone()) {
                if (!future.isCancelled()) {
                    try {
                        T taskResult = future.get();
                        successfullyCompletedTaskNumber++;
                    } catch (Exception e) {
                        failedTaskNumber++;
                    }
                }
            } else {
                future.cancel(true);
                inProgressTaskNumber++;
            }
        }
        if (inProgressTaskNumber == futureList.size() && throwTimeOutException && !isResultSet) {
            throw new TimeoutException("All tasks are still in progress, but timeout is over");
        }
        if (successfullyCompletedTaskNumber == 0 && !isResultSet) {
            throw new ExecutionException("No task successfully completed, failed: " + failedTaskNumber + ", in progress: " + inProgressTaskNumber, null);
        }
    }

    private <T> List<Future<T>> invokeAllAsync(Collection<? extends Callable<T>> tasks, final CountDownLatch timedLatch, List<Semaphore> semaphoreList) throws InterruptedException {
        List<Future<T>> futureList = new ArrayList<>();
        for (Callable<T> task : tasks) {
            Semaphore semaphore = new Semaphore(1);
            semaphoreList.add(semaphore);
            semaphore.acquire();
            TimedCallableTaskWrapper<T> taskWrapper = new TimedCallableTaskWrapper<>(task, result -> {
                semaphore.release();
                timedLatch.countDown();
            });
            futureList.add(submit(taskWrapper));
        }
        return futureList;
    }

    private <T> List<Future<T>> invokeAnyAsync(Collection<? extends Callable<T>> tasks, AnyResultHolder<T> resultHolder) {
        List<Future<T>> futureList = new ArrayList<>();
        for (Callable<T> task : tasks) {
            TimedCallableTaskWrapper<T> taskWrapper = new TimedCallableTaskWrapper<>(task, result -> resultHolder.setResult(result));
            futureList.add(submit(taskWrapper));
        }
        return futureList;
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
                log.debug("New Worker {} added", currentIndex);
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
