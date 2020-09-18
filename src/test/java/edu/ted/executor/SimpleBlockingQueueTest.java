package edu.ted.executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SimpleBlockingQueueTest {

    SimpleBlockingQueue<String> queue;

    @BeforeEach
    public void queueInit(){
        queue = new SimpleBlockingQueue<>(1);
    }

    @Test
    void add() {
        assertTrue(queue.add("First"));
        assertThrows(IllegalStateException.class, ()->queue.add("Second"));
    }

    @Test
    void addNullElement() {
        assertThrows(NullPointerException.class, ()->queue.add(null));
    }

    @Test
    void offer() {
        assertTrue(queue.offer("First"));
        assertFalse(queue.offer("Second"));
    }

    @Test
    void offerNullElement() {
        assertThrows(NullPointerException.class, ()->queue.offer(null));
    }
    @Test
    void put() throws InterruptedException {
        queue.put("First");
        long startTimePoint = System.currentTimeMillis();
        Runnable parallelTask = () -> {
            try {
                Thread.sleep(500);
                System.out.println(queue.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        new Thread(parallelTask).start();
        queue.put("Second");
        assertEquals("Second", queue.take());
        assertTrue((System.currentTimeMillis() - startTimePoint) >= 500);
    }

    @Test
    void testOffer() throws InterruptedException {
        queue.offer("First");
        long startTimePoint = System.currentTimeMillis();
        assertFalse(queue.offer("second", 1500, TimeUnit.MILLISECONDS));
        assertTrue((System.currentTimeMillis() - startTimePoint) >= 1500);

    }

    @Test
    void take() throws InterruptedException {
        queue.put("First");
        assertEquals("First", queue.take());
        long startTimePoint = System.currentTimeMillis();
        Runnable parallelTask = () -> {
            try {
                Thread.sleep(500);
                queue.put("Second");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        new Thread(parallelTask).start();
        assertEquals("Second", queue.take());
        assertTrue((System.currentTimeMillis() - startTimePoint) >= 500);
    }

    @Test
    void poll() throws InterruptedException {
        long startTimePoint = System.currentTimeMillis();
        String element = queue.poll(500, TimeUnit.MILLISECONDS);
        assertTrue((System.currentTimeMillis() - startTimePoint) > 500);
        assertNull(element);
        startTimePoint = System.currentTimeMillis();
        Runnable parallelTask = () -> {
            try {
                Thread.sleep(300);
                queue.put("Second");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        new Thread(parallelTask).start();
        element = queue.poll(500, TimeUnit.MILLISECONDS);
        assertEquals("Second", element);
        System.out.println(System.currentTimeMillis() - startTimePoint);
        assertTrue((System.currentTimeMillis() - startTimePoint) >= 300);
    }

    @Test
    void remainingCapacity() throws InterruptedException {
        assertEquals(1, queue.remainingCapacity());
        queue.put("First");
        assertEquals(0, queue.remainingCapacity());
    }

    @Test
    void remove() throws InterruptedException {
        queue.put("First");
        queue.remove("First");
        queue.put("Second");
        assertEquals("Second", queue.take());
    }

    @Test
    void removeNullElement() throws InterruptedException {
        queue.put("First");
        assertThrows(NullPointerException.class, ()->queue.remove(null));
    }

    @Test
    void contains() throws InterruptedException {
        String firstElement = "First";
        queue.put(firstElement);
        assertTrue(queue.contains(firstElement));
        assertFalse(queue.contains("Second"));
    }

    @Test
    void containsNullElement() throws InterruptedException {
        String firstElement = "First";
        queue.put(firstElement);
        assertThrows(NullPointerException.class, ()->queue.contains(null));
    }

    @Test
    void drainTo() throws InterruptedException {
        String firstElement = "First";
        queue.put(firstElement);
        List<String> drainingList = new ArrayList<>();
        assertEquals(1,queue.drainTo(drainingList));
        assertEquals(1, drainingList.size());
        assertNull(queue.poll(0, TimeUnit.MILLISECONDS));
    }

    @Test
    void givenEmptyQueue_whenDrainToAndCollectionIsEmpty_thenCorrect() throws InterruptedException {
        List<String> drainingList = new ArrayList<>();
        assertEquals(0, queue.drainTo(drainingList));
        assertTrue(drainingList.isEmpty());
        assertNull(queue.poll(0, TimeUnit.MILLISECONDS));
    }
}