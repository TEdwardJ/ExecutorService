package edu.ted.executorservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleBlockingQueue<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private T[] elementArray;
    private int head;
    private int tail;
    private int size;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition readCondition = lock.newCondition();
    private final Condition writeCondition = lock.newCondition();

    public SimpleBlockingQueue(int capacity) {
        this.elementArray = (T[]) new Object[capacity];
    }

    public boolean add(T t) {
        boolean result = offer(t);
        if (!result) {
            throw new IllegalStateException("No space is currently available");
        }
        return result;
    }

    public boolean offer(T t) {
        checkIfNull(t);
        lock.lock();
        try {
            if (size == elementArray.length) {
                return false;
            }
            addElement(t);
        } finally {
            lock.unlock();
        }
        return true;
    }

    public void put(T t) {
        checkIfNull(t);
        lock.lock();
        try {
            while (size == elementArray.length) {
                writeCondition.await();
            }
            addElement(t);
        } catch (InterruptedException e) {
            logger.debug("Method put() was interrupted", e);
        } finally {
            lock.unlock();
        }
    }

    public boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {
        checkTimeout(timeout);
        long waitTime = unit.toMillis(timeout);
        lock.lock();
        try {
            long time = System.currentTimeMillis();
            Date deadLine = new Date(time + waitTime);
            while (size == elementArray.length && System.currentTimeMillis() < deadLine.getTime()) {
                writeCondition.awaitUntil(deadLine);
            }
            if (size == elementArray.length) {
                return false;
            }
            addElement(t);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (size == 0) {
                readCondition.await();
            }
            return getElement();
        } finally {
            lock.unlock();
        }
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        checkTimeout(timeout);
        long waitTime = unit.toMillis(timeout);
        lock.lock();
        try {
            long time = System.currentTimeMillis();
            Date deadLine = new Date(time + waitTime);
            while (head == tail && System.currentTimeMillis() < deadLine.getTime()) {
                readCondition.awaitUntil(deadLine);
            }
            if (size == 0) {
                return null;
            }
            return getElement();
        } finally {
            lock.unlock();
        }
    }

    public int remainingCapacity() {
        lock.lock();
        try {
            return elementArray.length - size;
        } finally {
            lock.unlock();
        }
    }

    public int drainTo(Collection<? super T> c) {
        lock.lock();
        try {
            for (int i = 0; i < size; i++) {
                c.add(getElement());
            }
        } finally {
            lock.unlock();
        }
        return c.size();
    }

    public int drainTo(Collection<? super T> c, int maxElements) {
        lock.lock();
        try {
            for (int i = 0; i < Math.min(size, maxElements); i++) {
                c.add(getElement());
            }
        } finally {
            lock.unlock();
        }
        return c.size();
    }


    private void checkIfNull(Object o) {
        if (Objects.isNull(o)) {
            throw new NullPointerException();
        }
    }

    private void checkTimeout(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Argument timeout cannot be less than zero: " + timeout);
        }
    }

    private void defragmentationAfterRemove(int removedIndex) {
        int newHead = 0;
        int newTail = 0;

        for (int i = removedIndex; i < size - 1; i++) {
            int index = alignIndexToArrayBoundaries(i + 1);
            int newIndex = alignIndexToArrayBoundaries(i);
            elementArray[newIndex] = elementArray[index];

        }
        head = newHead;
        tail = newTail;
    }

    private int alignIndexToArrayBoundaries(int index) {
        if (index >= elementArray.length) {
            index = index - elementArray.length;
        }
        return index;
    }

    private void addElement(T t) {
        elementArray[tail] = t;
        tail = alignIndexToArrayBoundaries(++tail);
        size++;
        readCondition.signal();
    }

    private T getElement() {
        T element = getElement(head);
        head++;
        head = alignIndexToArrayBoundaries(head);
        writeCondition.signal();
        return element;
    }

    private T getElement(int index) {
        T element = elementArray[index];
        size--;
        return element;
    }
}
