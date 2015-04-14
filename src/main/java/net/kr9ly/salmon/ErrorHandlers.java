package net.kr9ly.salmon;

import net.kr9ly.salmon.error.OnEventError;
import net.kr9ly.salmon.event.EventState;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
class ErrorHandlers {

    Set<OnEventError> errorHandlers = new HashSet<>();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    private final Lock readLock = rwLock.readLock();

    private final Lock writeLock = rwLock.writeLock();

    /* package */ void add(OnEventError onEventError) {
        writeLock.lock();
        try {
            errorHandlers.add(onEventError);
        } finally {
            writeLock.unlock();
        }
    }

    /* package */ EventState handleError(TreeEventBus eventBus, Throwable e) {
        readLock.lock();
        try {
            for (OnEventError errorHandler : errorHandlers) {
                if (eventBus.isShutdown) {
                    return EventState.PASS;
                }
                EventState state = errorHandler.onEventError(e);
                if (state == EventState.RESOLVE) {
                    return EventState.RESOLVE;
                }
            }
        } finally {
            readLock.unlock();
        }
        if (eventBus.parent != null && !eventBus.isShutdown) {
            return eventBus.parent.handleError(e);
        }
        return EventState.PASS;
    }

    /* package */ void clear() {
        writeLock.lock();
        try {
            errorHandlers.clear();
        } finally {
            writeLock.unlock();
        }
    }
}
