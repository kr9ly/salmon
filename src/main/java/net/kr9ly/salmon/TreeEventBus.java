package net.kr9ly.salmon;

import net.kr9ly.salmon.dispatcher.EventDispatcher;
import net.kr9ly.salmon.error.OnEventError;
import net.kr9ly.salmon.event.Event;
import net.kr9ly.salmon.event.EventState;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
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
public class TreeEventBus {

    /**
     * New root.
     *
     * @param dispatcher the dispatcher
     * @return the tree event bus
     */
    public static TreeEventBus newRoot(EventDispatcher dispatcher) {
        return new TreeEventBus(null, dispatcher);
    }

    private final TreeEventBus parent;

    private final EventDispatcher dispatcher;

    private final Map<Class<?>, Set<Object>> events = new HashMap<>();

    private final Set<OnEventError> errorHandlers = Collections.synchronizedSet(new HashSet<OnEventError>());

    private final EventInvocationHandler invocationHandler;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    private final Lock readLock = rwLock.readLock();

    private final Lock writeLock = rwLock.writeLock();

    private volatile boolean isShutdown = false;

    private TreeEventBus(TreeEventBus parent, EventDispatcher dispatcher) {
        this.parent = parent;
        this.dispatcher = dispatcher;
        invocationHandler = new EventInvocationHandler(dispatcher, this);
    }

    /**
     * New child.
     *
     * @return the tree event bus
     */
    public TreeEventBus newChild() {
        return new TreeEventBus(this, dispatcher);
    }

    /**
     * Subscribe void.
     *
     * @param subscriber the subscriber
     */
    public void subscribe(Object subscriber) {
        for (Class<?> eventClass : subscriber.getClass().getInterfaces()) {
            if (eventClass.equals(OnEventError.class)) {
                errorHandlers.add((OnEventError) subscriber);
            }
            if (eventClass.getAnnotation(Event.class) == null) {
                continue;
            }
            Set<Object> subscribers = getSubscribers(eventClass);
            subscribers.add(subscriber);
        }
    }

    /**
     * Gets publisher.
     *
     * @param eventClass the event class
     * @return the publisher
     */
    public <I> I getPublisher(Class<I> eventClass) {
        Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{eventClass}, invocationHandler);
        return eventClass.cast(proxy);
    }

    /**
     * Shutdown void.
     */
    public void shutdown() {
        writeLock.lock();
        try {
            isShutdown = true;
            events.clear();
            errorHandlers.clear();
        } finally {
            writeLock.unlock();
        }
    }

    private Set<Object> getSubscribers(Class<?> eventClass) {
        readLock.lock();
        try {
            Set<Object> subscribers = events.get(eventClass);
            if (subscribers == null) {
                readLock.unlock();
                writeLock.lock();
                try {
                    if (!events.containsKey(eventClass)) {
                        subscribers = Collections.synchronizedSet(new HashSet<>());
                        events.put(eventClass, subscribers);
                    } else {
                        subscribers = events.get(eventClass);
                    }
                } finally {
                    readLock.lock();
                    writeLock.unlock();
                }
            }
            return subscribers;
        } finally {
            readLock.unlock();
        }
    }

    /* package */ EventState dispatchEvent(Class<?> eventClass, Method method, Object[] args) {
        for (Object subcriber : getSubscribers(eventClass)) {
            try {
                if (isShutdown) {
                    return EventState.PASS;
                }
                EventState state = (EventState) method.invoke(subcriber, args);
                if (state == EventState.RESOLVE) {
                    return EventState.RESOLVE;
                }
            } catch (ClassCastException e) {
                throw new Error("Event return type Must be EventState.");
            } catch (IllegalAccessException e) {
                throw new Error(e);
            } catch (InvocationTargetException e) {
                return handleError(e.getCause());
            }
        }
        if (parent != null && !isShutdown) {
            return parent.dispatchEvent(eventClass, method, args);
        }
        return EventState.PASS;
    }

    private EventState handleError(Throwable e) {
        for (OnEventError errorHandler : errorHandlers) {
            if (isShutdown) {
                return EventState.PASS;
            }
            EventState state = errorHandler.onEventError(e);
            if (state == EventState.RESOLVE) {
                return EventState.RESOLVE;
            }
        }
        if (parent != null && !isShutdown) {
            return parent.handleError(e);
        }
        return EventState.PASS;
    }
}
