package edu.ted.executorservice;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        ExecutorService service1 = Executors.newFixedThreadPool(1);
        ExecutorService service = new SimpleFixedThreadPool(3);
        service.submit(() -> parallelTask(2000));
        service.submit(() -> parallelTask(4000));
        sleep(3000);
        service.shutdown();
        service.submit(() -> parallelTask(500));
    }

    private static void parallelTask(long sleepPeriod) {
        sleep(sleepPeriod);
        System.out.println("Parallel Task for " + sleepPeriod + " is to finish");
    }

    private static void sleep(long period) {
        try {
            Thread.sleep(period);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
