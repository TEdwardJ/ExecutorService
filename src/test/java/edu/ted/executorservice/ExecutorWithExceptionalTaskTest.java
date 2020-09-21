package edu.ted.executorservice;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutorWithExceptionalTaskTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void givenTasksThrowingExceptions_whenGetExceptionFromTheFuture_thenCorrect() throws InterruptedException {

        int taskNumber = 4;
        final ExecutorService executor = new SimpleFixedThreadPool(4);
        final List<Future<?>> futureList = new ArrayList<>();
        final CountDownLatch finishLatch = new CountDownLatch(taskNumber);
        for (int i = 0; i < taskNumber; i++) {
            final int num = i;
            logger.debug("giving tasks");
            Runnable runnable = () -> {
                logger.debug("Task number {} is executing", num);
                try {
                    Thread.sleep(300);
                    throw new RuntimeException("TestException. Something went wrong");
                } catch (InterruptedException e) {
                    logger.debug("Interrupted: ", e);
                } finally {
                    finishLatch.countDown();
                }
            };
            futureList.add(executor.submit(runnable));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.debug("Interrupted: ", e);
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
