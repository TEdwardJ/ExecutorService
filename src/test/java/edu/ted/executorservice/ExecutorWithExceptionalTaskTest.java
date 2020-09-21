package edu.ted.executorservice;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class ExecutorWithExceptionalTaskTest {

    @Test
    public void givenTasksThrowingExceptions_whenGetExceptionFromTheFuture_thenCorrect() throws InterruptedException {

        int taskNumber = 4;
        final ExecutorService executor = new SimpleFixedThreadPool(4);
        final List<Future<?>> futureList = new ArrayList<>();
        final CountDownLatch finishLatch = new CountDownLatch(taskNumber);
        for (int i = 0; i < taskNumber; i++) {
            final int num = i;
            log.debug("giving tasks");
            Runnable runnable = () -> {
                log.debug("Task number {} is executing", num);
                try {
                    Thread.sleep(300);
                    throw new RuntimeException("TestException. Something went wrong");
                } catch (InterruptedException e) {
                    log.debug("Interrupted: ", e);
                } finally {
                    finishLatch.countDown();
                }
            };
            futureList.add(executor.submit(runnable));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.debug("Interrupted: ", e);
            }
        }
        finishLatch.await();
        for (Future<?> future : futureList) {
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
            assertThrows(ExecutionException.class, future::get);
        }
    }
}
