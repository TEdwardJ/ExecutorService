package edu.ted.executorservice;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class SimpleLinkedBlockingQueue<T> {

    //private final Logger log = LoggerFactory.getLogger(getClass());

    private final int capacity;
    private Node<T> tail;
    private Node<T> head;
    private int size;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition readCondition = lock.newCondition();
    private final Condition writeCondition = lock.newCondition();

    public SimpleLinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    public SimpleLinkedBlockingQueue(int capacity) {
        this.capacity = capacity;
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
            if (size == capacity) {
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
            while (size == capacity) {
                writeCondition.await();
            }
            addElement(t);
        } catch (InterruptedException e) {
            log.debug("Method put() was interrupted", e);
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
            while (size == capacity && System.currentTimeMillis() < deadLine.getTime()) {
                writeCondition.awaitUntil(deadLine);
            }
            if (size == capacity) {
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
            return capacity - size;
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

    private void addElement(T t) {
        Node<T> elementNode = new Node<>(t);
        if (size == 0){
            head = tail = elementNode;
        } else {
            tail.next = elementNode;
            tail = tail.next;
        }
        size++;
        readCondition.signal();
    }

    private T getElement() {
        T element = head.value;
        size--;
        head = head.next;
        writeCondition.signal();
        return element;
    }

    private static class Node<T> {
        private Node<T> next;
        private T value;

        private Node(T value) {
            this.value = value;
        }
    }
}