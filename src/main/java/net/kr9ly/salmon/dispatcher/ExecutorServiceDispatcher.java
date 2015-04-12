package net.kr9ly.salmon.dispatcher;

import net.kr9ly.salmon.event.EventState;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

/**
 * Copyright 2015 kr9ly
 * <br />
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <br />
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br />
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ExecutorServiceDispatcher implements EventDispatcher {

    private final ExecutorService executorService;

    public ExecutorServiceDispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public EventState dispatch(EventInvoker invoker, Method method, Object[] args) {
        if (executorService.isShutdown()) {
            return EventState.PASS;
        }
        executorService.submit(new DispatchRunnable(invoker, method, args));
        return EventState.RESOLVE;
    }

    private class DispatchRunnable implements Runnable {

        private final EventInvoker invoker;

        private final Method method;

        private final Object[] args;

        private DispatchRunnable(EventInvoker invoker, Method method, Object[] args) {
            this.invoker = invoker;
            this.method = method;
            this.args = args;
        }

        @Override
        public void run() {
            invoker.invoke(method, args);
        }
    }
}
