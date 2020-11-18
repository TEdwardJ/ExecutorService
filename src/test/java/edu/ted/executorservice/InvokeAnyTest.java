package edu.ted.executorservice;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class InvokeAnyTest {

    @Test
    public void invokeAnyWithTimeoutLessThanTask_whenTimeoutError_thenCorrect() {
        int taskNumber = 11;
        final ExecutorService executor = new SimpleFixedThreadPool(4);
        List<Callable<Integer>> tasksList = new ArrayList<>();
        for (int i = 0; i < taskNumber; i++) {
            final int num = i + 1;
            log.debug("giving tasks");
            Callable<Integer> callable = () -> {
                try {
                    System.out.println("Task number " + num + " is executing for approximately " + 80);
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    System.out.println("Task number " + num + " is interrupted");
                }
                return num;
            };
            tasksList.add(callable);
        }
        assertThrows(TimeoutException.class, () -> executor.invokeAny(tasksList, 30, TimeUnit.MILLISECONDS));
    }

    @Test
    public void invokeAnyWithTimeoutTest() throws InterruptedException, ExecutionException, TimeoutException {
        int taskNumber = 11;
        int result;
        final ExecutorService executor = new SimpleFixedThreadPool(4);
        List<Callable<Integer>> tasksList = new ArrayList<>();
        for (int i = 0; i < taskNumber; i++) {
            final int num = i + 1;
            log.debug("giving tasks");
            Callable<Integer> callable = () -> {
                try {
                    System.out.println("Task number " + num + " is executing for approximately " + 300);
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println("Task number " + num + " is interrupted");
                }
                return num;
            };
            tasksList.add(callable);
        }
        long timePoint = System.currentTimeMillis();
        result = executor.invokeAny(tasksList, 600, TimeUnit.MILLISECONDS);

        assertTrue((System.currentTimeMillis()) - timePoint >= 500);

        assertNotNull(result);
        log.debug("The Result {}", result);
        assertTrue(result > 0);
        assertTrue(result < taskNumber);
    }

    @Test
    public void invokeAnyWithoutTimeoutTest() throws ExecutionException, InterruptedException {
        int taskNumber = 11;
        int result;
        final ExecutorService executor = new SimpleFixedThreadPool(4);
        List<Callable<Integer>> tasksList = new ArrayList<>();
        for (int i = 0; i < taskNumber; i++) {
            final int num = i + 1;
            log.debug("giving tasks");
            Callable<Integer> callable = () -> {
                try {
                    System.out.println("Task number " + num + " is executing for approximately " + 80);
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    System.out.println("Task number " + num + " is interrupted");
                }
                return num;
            };
            tasksList.add(callable);
        }
        long timePoint = System.currentTimeMillis();

        result = executor.invokeAny(tasksList);


        assertTrue((System.currentTimeMillis()) - timePoint >= 80);

        assertNotNull(result);
        log.debug("The Result {}", result);
        assertTrue(result > 0);
        assertTrue(result < taskNumber);
    }
}
