package net.kr9ly.salmon;

import net.kr9ly.salmon.dispatcher.EventDispatcher;
import net.kr9ly.salmon.dispatcher.EventInvoker;
import net.kr9ly.salmon.event.EventState;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

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
/* package */ class EventInvocationHandler implements InvocationHandler, EventInvoker {

    private EventDispatcher dispatcher;

    private TreeEventBus eventBus;

    public EventInvocationHandler(EventDispatcher dispatcher, TreeEventBus eventBus) {
        this.dispatcher = dispatcher;
        this.eventBus = eventBus;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return dispatcher.dispatch(this, method, args);
    }

    @Override
    public EventState invoke(Method method, Object[] args) {
        return eventBus.dispatchEvent(method.getDeclaringClass(), method, args);
    }
}
