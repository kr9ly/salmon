# Salmon - A hierarchical event bus

[![Circle CI](https://circleci.com/gh/kr9ly/salmon/tree/master.svg?style=svg)](https://circleci.com/gh/kr9ly/salmon/tree/master)

A hierarchical event bus.

# Usage

Add this to `repositories` block in your build.gradle

```
maven { url 'http://kr9ly.github.io/maven/' }
```

And Add this to `dependencies` block in your build.gradle

```
compile 'net.kr9ly:salmon:0.0.1'
```

### Basic

##### 1. Create interface for event subscription.

```java
@Event // annotate by net.kr9ly.salmon.event.Event
public interface SampleEvent {
    // return type must be net.kr9ly.salmon.event.EventState
    // you can define any arguments
    EventState callSampleEvent(String message);
}
```

##### 2. Create subscriber class implemented your event interfaces.

```java
public class SampleSubscriber implements SampleEvent {
    @Override
    public EventState callSampleEvent(String message) {
        System.out.println(message);
        // return EventState.RESOLVE if you want to stop event propagation
        return EventState.PASS;
    }
}
```

##### 3. Create event bus with dispatcher.

```java
TreeEventBus eventBus = TreeEventBus.newRoot(Dispatchers.immediate());
// or async dispatch
TreeEventBus eventBus = TreeEventBus.newRoot(Dispatchers.async(Executors.newFixedThreadPool(1)));
```

##### 4. Subscribe event by your subscriber class.

```java
eventBus.subscribe(new SampleSubscriber());
```

##### 5. Publish event by publisher.

```java
SampleEvent publisher = eventBus.getPublisher(SampleEvent.class);
publisher.callSampleEvent("this is sample event call.");
// -> this is sample event call.
```

### Handling child events

```java
TreeEventBus parentBus = TreeEventBus.newRoot(Dispatchers.immediate());
TreeEventBus childBus = parentBus.newChild();
parentBus.subscribe(new SampleSubscriber());

childBus.getPublisher(SampleEvent.class).callSampleEvent("this is child event call.");
// -> this is child event call.
```

### Handling exceptions

##### 1. Implement net.kr9ly.salmon.error.OnEventError interface to your subscriber class.

```java
public class SampleErrorHandler implements OnEventError {
    @Override
    EventState onEventError(Throwable e) {
        System.out.println("catch throwable.");
        // return EventState.RESOLVE if you want to stop error propagation
        return EventState.PASS;
    }
}
```

##### 2. Subscribe.

```java
TreeEventBus bus = TreeEventBus.newRoot(Dispatchers.immediate());
bus.subscribe(new SampleSubscriber()); // subscriber will cause error.
bus.subscribe(new SampleErrorHandler());

bus.getPublisher(SampleErrorEvent.class).causeErrorEvent();
// -> catch throwable.
```

# License

```
Copyright 2015 kr9ly

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```