package edu.ted.executorservice;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

@Slf4j
public class TimedCallableTaskWrapper<T> implements Callable<T> {

    private final Callable<T> callable;
    private Consumer<T> onResultAction;

    public TimedCallableTaskWrapper(Callable<T> wrappedCallable, Consumer<T> onResultAction) {
        this.callable = wrappedCallable;
        this.onResultAction = onResultAction;
    }

    @Override
    public T call() throws Exception {
        T result = null;
        try {
            result = callable.call();
            log.debug("Result is set: {}", result);
            return result;
        } finally {
            onResultAction.accept(result);
        }
    }
}
