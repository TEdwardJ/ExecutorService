package edu.ted.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleFixedThreadPool implements ExecutorService {
    private final ReentrantLock shutdownLock = new ReentrantLock();
    private final CountDownLatch terminationLatch;
    private final SimpleBlockingQueue<FutureTask<?>> queue = new SimpleBlockingQueue<FutureTask<?>>(100);
    private final List<Thread> executorsThreadPool;
    private final List<SimpleExecutor> executorList;
    private volatile boolean isInWork;

    public SimpleFixedThreadPool(int poolSize) {
        CyclicBarrier poolInitiationBarrier = new CyclicBarrier(poolSize);
        terminationLatch = new CountDownLatch(poolSize);
        executorsThreadPool = new ArrayList<>(poolSize);
        executorList = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            final SimpleExecutor executor = new SimpleExecutor(queue, "Executor#" + i, poolInitiationBarrier, terminationLatch);
            Thread executionThread = new Thread(executor);

            executorsThreadPool.add(executionThread);
            executorList.add(executor);
            executionThread.start();
        }
        isInWork = true;
    }

    public void shutdown() {
        shutdownLock.lock();
        softShutdownThreads();
        shutdownLock.unlock();
    }

    private void softShutdownThreads() {
        if (!isInWork){
            return;
        }
        isInWork = false;
        for (SimpleExecutor simpleExecutor : executorList) {
            simpleExecutor.shutdown();
        }
    }

    public List<Runnable> shutdownNow() {
        shutdownLock.lock();
        softShutdownThreads();
        final List<Runnable> restOfTasksList = new ArrayList<>();
        queue.drainTo(restOfTasksList);
        for (Thread thread : executorsThreadPool) {
            thread.interrupt();
        }
        shutdownLock.unlock();
        return restOfTasksList;
    }

    public boolean isShutdown() {
        return !isInWork;
    }

    public boolean isTerminated() {
        return terminationLatch.getCount() == 0;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return terminationLatch.await(timeout, unit);
    }

    public <T> Future<T> submit(Callable<T> task) {
        final FutureTask<T> futureTask = new FutureTask<>(task);
        try {
            queue.put(futureTask);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return futureTask;
    }


    public <T> Future<T> submit(Runnable task, T result) {
        final FutureTask<T> futureTask = new FutureTask<>(task, result);
        try {
            queue.put(futureTask);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return futureTask;
    }

    public Future<?> submit(Runnable task) {
        return submit(task, true);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        CountDownLatch timedLatch = new CountDownLatch(tasks.size());
        List<Future<T>> futureList = new ArrayList<>();
        for (Callable<T> task : tasks) {
            CallableTaskWrapper<T> taskWrapper = new CallableTaskWrapper<>(task, timedLatch);
            futureList.add(submit(taskWrapper));
        }
        timedLatch.await();
        return futureList;
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch timedLatch = new CountDownLatch(tasks.size());
        List<Future<T>> futureList = new ArrayList<>();
        for (Callable<T> task : tasks) {
            CallableTaskWrapper<T> taskWrapper = new CallableTaskWrapper<>(task, timedLatch);
            futureList.add(submit(taskWrapper));
        }
        timedLatch.await(timeout, unit);
        futureList.forEach(t-> t.cancel(true));
        System.out.println("invokeAll Timeout is over");
        return futureList;
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        List<Future<T>> futureList = invokeAll(tasks);
        for (Future<T> result : futureList) {
            if (result.isDone() && !result.isCancelled()){
                return result.get();
            }
        }
        throw new ExecutionException("No task successfully completes", null);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        List<Future<T>> futureList = invokeAll(tasks, timeout, unit);
        for (Future<T> result : futureList) {
            if (result.isDone() && !result.isCancelled()){
                return result.get();
            }
        }
        throw new ExecutionException("No task successfully completes", null);
    }

    public void execute(Runnable command) {
        submit(command);
    }
}
