package edu.ted.executor;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleBlockingQueue<T> {
    private final T[] elementArray;
    private int head;
    private int tail;
    private int size;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition readCondition = lock.newCondition();
    private final Condition writeCondition = lock.newCondition();

    public SimpleBlockingQueue(int size) {
        this.elementArray = (T[]) new Object[size];
    }

    public boolean add(T t) {
        boolean result = offer(t);
        if (!result) {
            throw new IllegalStateException("Nno space is currently available");
        }
        return result;
    }

    public boolean offer(T t) {
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

    private void addElement(T t) {
        elementArray[tail++] = t;
        size++;
        readCondition.signal();
    }

    private T getElement() {
        T element =  getElement(head);
        head++;
        head = getIndex(head);
        tail = getIndex(tail);

        return element;
    }

    private T getElement(int index) {
        T element = elementArray[index];
        size--;
        writeCondition.signal();
        return element;
    }

    public void put(T t) throws InterruptedException {
        lock.lock();
        try {
            while (size == elementArray.length) {
                writeCondition.await();
            }
            addElement(t);
        } finally {
            lock.unlock();
        }
    }

    public boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("Argument timeout cannot be less than zero: " + timeout);
        }
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
            while (head == tail) {
                readCondition.await();
            }
            return getElement();
        } finally {
            lock.unlock();
        }
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("Argument timeout cannot be less than zero: " + timeout);
        }
        long waitTime = unit.toMillis(timeout);
        lock.lock();
        try {
            long time = System.currentTimeMillis();
            Date deadLine = new Date(time + waitTime);
            while (head == tail && System.currentTimeMillis() < deadLine.getTime()) {
                readCondition.awaitUntil(deadLine);
            }
            if (head == tail) {
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

    public boolean remove(Object o) {
        lock.lock();
        int removedIndex;
        try {
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    int index = getIndex(head + i);
                    if (Objects.equals(o, elementArray[index])) {
                        removedIndex = index;
                        elementArray[index] = null;
                        defragmentationAfterRemove(removedIndex);
                        size--;
                        writeCondition.signal();
                        return true;
                    }
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void defragmentationAfterRemove(int removedIndex) {
        int newHead = 0;
        int newTail = 0;


        for (int i = removedIndex; i < size-1; i++) {
            int index = getIndex(i + 1);
            int newIndex = getIndex(i);
            elementArray[newIndex] = elementArray[index];

        }
        head = newHead;
        tail = newTail;
    }

    private int getIndex(int index) {
        if (index >= elementArray.length) {
            index = index - elementArray.length;
        }
        return index;
    }

    public boolean contains(Object o) {
        lock.lock();
        try {
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    int index = getIndex(head + i);
                    if (Objects.equals(o, elementArray[index])) {
                        return true;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        return false;
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
}
