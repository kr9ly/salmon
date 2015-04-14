package net.kr9ly.salmon;

import net.kr9ly.salmon.event.EventState;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
class Subscribers {

    private Set<Object> subscribers = new HashSet<>();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    private final Lock readLock = rwLock.readLock();

    private final Lock writeLock = rwLock.writeLock();

    /* package */ void add(Object subscriber) {
        writeLock.lock();
        try {
            subscribers.add(subscriber);
        } finally {
            writeLock.unlock();
        }
    }

    /* package */ EventState dispatchEvent(TreeEventBus eventBus, Class<?> eventClass, Method method, Object[] args) {
        readLock.lock();
        try {
            for (Object subscriber : subscribers) {
                try {
                    if (eventBus.isShutdown) {
                        return EventState.PASS;
                    }
                    EventState state = (EventState) method.invoke(subscriber, args);
                    if (state == EventState.RESOLVE) {
                        return EventState.RESOLVE;
                    }
                } catch (ClassCastException e) {
                    throw new Error("Event return type Must be EventState.");
                } catch (IllegalAccessException e) {
                    throw new Error(e);
                } catch (InvocationTargetException e) {
                    return eventBus.handleError(e.getCause());
                }
            }
        } finally {
            readLock.unlock();
        }
        if (eventBus.parent != null && !eventBus.isShutdown) {
            return eventBus.parent.dispatchEvent(eventClass, method, args);
        }
        return EventState.PASS;
    }
}
