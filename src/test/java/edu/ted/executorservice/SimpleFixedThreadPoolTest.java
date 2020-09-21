package edu.ted.executorservice;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleFixedThreadPoolTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Test
    public void testRunnableInBulk() throws InterruptedException {
        int taskNumber = 11;
        final CountDownLatch finishLatch = new CountDownLatch(taskNumber);
        final ExecutorService executor = new SimpleFixedThreadPool(4);
        final Map<Integer, Boolean> resultMap = new ConcurrentHashMap<>();
        List<Future<?>> futureList = new ArrayList<>();
        for (int i = 0; i < taskNumber; i++) {
            final int num = i;
            logger.debug("giving tasks");
            Runnable runnable = () -> {
                logger.debug("Task number {} is executing", num);
                try {
                    Thread.sleep(500);
                    resultMap.put(num, true);
                } catch (InterruptedException e) {
                    logger.debug("Interrupted: ", e);
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
            logger.debug("Interrupted: ", e);
        }
        for (Future<?> future : futureList) {
            try {
                assertTrue(future.isDone());
                assertFalse(future.isCancelled());
                assertTrue((Boolean) future.get());
            } catch (ExecutionException | InterruptedException e) {
                logger.debug("Exception: ", e);
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
            logger.debug("giving tasks");
            Callable<Integer> callable = () -> {
                logger.debug("Task number {} is executing", num);
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
            logger.debug("Interrupted: ", e);
        }
        for (Future<?> future : futureList) {
            try {
                assertTrue(future.isDone());
                assertFalse(future.isCancelled());
                Integer intResult = (Integer) future.get();
                resultMap.put(intResult, true);
                assertTrue(intResult >= 0);
            } catch (ExecutionException | InterruptedException e) {
                logger.debug("Exception: ", e);
            }
        }
        assertEquals(taskCount, resultMap.size());
        for (int i = 0; i < taskCount; i++) {
            assertTrue(resultMap.containsKey(i));
        }
    }
}
