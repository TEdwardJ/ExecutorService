package edu.ted.executor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class InvokeAllTest {

    @Test
    public void test() throws InterruptedException {
        int taskNumber = 11;
        final ExecutorService executor = new SimpleFixedThreadPool(4);
        final Map<Integer, Boolean> resultMap = new ConcurrentHashMap<>();
        final CountDownLatch finishLatch = new CountDownLatch(taskNumber);
        List<Callable<Integer>> tasksList = new ArrayList<>();
        List<Future<Integer>> futureList = new ArrayList<>();
        for (int i = 0; i < taskNumber; i++) {
            final int num = i;
            System.out.println("giving tasks");
            Callable<Integer> callable = () -> {
                try {
                    System.out.println("Task number " + num + " is executing for approximately " + 100);
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    System.out.println("Task number " + num + " is interrupted");
                }
                finishLatch.countDown();
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
        assertTrue((System.currentTimeMillis()) - timePoint >= 300);
        finishLatch.await();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Future<Integer> future : futureList) {
            try {
                assertTrue(future.isDone());
                assertFalse(future.isCancelled());
                Integer intResult = (Integer) future.get();
                resultMap.put(intResult, true);
                assertTrue(intResult >= 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                System.out.println(" Interrupted while waiting");
            }
        }

        assertEquals(taskNumber, resultMap.size());
        for (int i = 0; i < taskNumber; i++) {
            assertTrue(resultMap.containsKey(i));
        }
    }
}
