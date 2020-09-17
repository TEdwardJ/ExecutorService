package edu.ted.executor;

import edu.ted.executor.SimpleFixedThreadPool;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleFixedThreadPoolTest {

    @Test
    public void testRunnableInBulk() throws InterruptedException {
        int taskNumber = 11;
        final CountDownLatch finishLatch = new CountDownLatch(taskNumber);
        final ExecutorService executor = new SimpleFixedThreadPool(4);
        final Map<Integer, Boolean> resultMap = new ConcurrentHashMap<>();
        List<Future<?>> futureList = new ArrayList<>();
        for (int i = 0; i < taskNumber; i++) {
            final int num = i;
            System.out.println("giving tasks");
            Runnable runnable = () -> {
                System.out.println("Task number " + num + " is executing");
                try {
                    Thread.sleep(500);
                    resultMap.put(num, true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            };
            futureList.add(executor.submit(runnable));

        }
        finishLatch.await();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Future<?> future : futureList) {
            try {
                assertTrue(future.isDone());
                assertFalse(future.isCancelled());
                assertTrue((Boolean) future.get());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < taskNumber; i++) {
            assertTrue(resultMap.containsKey(i));
        }
    }

    @Test
    public void testCallableInBulk() throws InterruptedException {
        int taskCount = 11;
        final CountDownLatch finishLatch = new CountDownLatch(taskCount);
        final ExecutorService executor = new SimpleFixedThreadPool(4);
        final Map<Integer, Boolean> resultMap = new ConcurrentHashMap<>();
        List<Future<Integer>> futureList = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {

            final int num = i;
            System.out.println("giving tasks");
            Callable<Integer> callable = () -> {
                System.out.println("Task number " + num + " is executing");
                try {
                    Thread.sleep(500);
                    return num;
                } catch (InterruptedException e) {
                    throw e;
                } finally {
                    finishLatch.countDown();
                }
            };
            futureList.add(executor.submit(callable));

        }
        finishLatch.await();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Future<?> future : futureList) {
            try {
                assertTrue(future.isDone());
                assertFalse(future.isCancelled());
                Integer intResult = (Integer) future.get();
                resultMap.put(intResult, true);
                assertTrue(intResult >= 0);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertEquals(taskCount, resultMap.size());
        for (int i = 0; i < taskCount; i++) {
            assertTrue(resultMap.containsKey(i));
        }
    }
}
