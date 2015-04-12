package net.kr9ly.salmon.dispatcher;

import java.util.concurrent.ExecutorService;

public final class Dispatchers {

    /**
     * Immediate event dispatcher.
     *
     * @return the event dispatcher
     */
    public static EventDispatcher immediate() {
        return new ImmediateDispatcher();
    }

    /**
     * Async event dispatcher.
     *
     * @param executorService the executor service
     * @return the event dispatcher
     */
    public static EventDispatcher async(ExecutorService executorService) {
        return new ExecutorServiceDispatcher(executorService);
    }
}
