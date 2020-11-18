package edu.ted.executorservice;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class InvokeAllTest {

    @Test
    public void invokeAllWithTimeoutTest() throws InterruptedException {
        int taskNumber = 11;
        final ExecutorService executor = new SimpleFixedThreadPool(4);
        final Map<Integer, Boolean> resultMap = new ConcurrentHashMap<>();
        List<Callable<Integer>> tasksList = new ArrayList<>();
        List<Future<Integer>> futureList = new ArrayList<>();
        for (int i = 0; i < taskNumber; i++) {
            final int num = i;
            log.debug("giving tasks");
            Callable<Integer> callable = () -> {
                try {
                    log.debug("Task number {} is executing for approximately {}", num, 80);
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    log.debug("Task number {} is interrupted", num);
                }
                return num;
            };
            tasksList.add(callable);
        }
        long timePoint = System.currentTimeMillis();
        try {
            futureList = executor.invokeAll(tasksList, 500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue((System.currentTimeMillis()) - timePoint >= 240);
        for (Future<Integer> future : futureList) {
            try {
                assertTrue(future.isDone());
                assertFalse(future.isCancelled());
                Integer intResult = future.get();
                resultMap.put(intResult, true);
                assertTrue(intResult >= 0);
            } catch (ExecutionException e) {
                log.error(" Interrupted while waiting", e);
            }
        }

        assertEquals(taskNumber, resultMap.size());
        for (int i = 0; i < taskNumber; i++) {
            assertTrue(resultMap.containsKey(i));
        }
    }

    @Test
    public void invokeAllWithoutTimeoutTest() throws InterruptedException {
        int taskNumber = 11;
        final ExecutorService executor = new SimpleFixedThreadPool(4);
        final Map<Integer, Boolean> resultMap = new ConcurrentHashMap<>();
        List<Callable<Integer>> tasksList = new ArrayList<>();
        List<Future<Integer>> futureList = new ArrayList<>();
        for (int i = 0; i < taskNumber; i++) {
            final int num = i;
            log.debug("giving tasks");
            Callable<Integer> callable = () -> {
                try {
                    log.debug("Task number {} is executing for approximately {}", num, 100);
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.debug("Task number {} is interrupted", num);
                }
                return num;
            };
            tasksList.add(callable);
        }
        long timePoint = System.currentTimeMillis();
        try {
            futureList = executor.invokeAll(tasksList);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue((System.currentTimeMillis()) - timePoint >= 300);
        for (Future<Integer> future : futureList) {
            try {
                assertTrue(future.isDone());
                assertFalse(future.isCancelled());
                Integer intResult = future.get();
                resultMap.put(intResult, true);
                assertTrue(intResult >= 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                log.debug("Interrupted while waiting", e);
            }
        }

        assertEquals(taskNumber, resultMap.size());
        for (int i = 0; i < taskNumber; i++) {
            assertTrue(resultMap.containsKey(i));
        }
    }
}
