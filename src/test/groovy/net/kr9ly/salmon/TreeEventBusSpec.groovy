package net.kr9ly.salmon

import net.kr9ly.salmon.dispatcher.Dispatchers
import net.kr9ly.salmon.error.OnEventError
import net.kr9ly.salmon.event.Event
import net.kr9ly.salmon.event.EventState
import spock.lang.Specification

class TreeEventBusSpec extends Specification {

    def "call registered event through interfaces"() {
        setup:
        def bus = TreeEventBus.newRoot(Dispatchers.immediate())
        def subscriber = new TestSubscriber()
        bus.subscribe(subscriber)

        when:
        def shouldBeResolve = bus.getPublisher(EventA.class).callEventA(message)
        def shouldBeMessage = subscriber.messageA
        def shouldBeEmpty = subscriber.messageB
        def shouldBePass = bus.getPublisher(EventB.class).callEventB(message)
        def shouldBeMessageB = subscriber.messageB
        def shouldBePassC = bus.getPublisher(EventC.class).callEventC(message)

        then:
        shouldBeResolve == EventState.RESOLVE
        shouldBeMessage == message
        shouldBeEmpty == ""
        shouldBePass == EventState.PASS
        shouldBeMessageB == message
        shouldBePassC == EventState.PASS

        where:
        message | _
        "test"  | _
    }

    def "call registered event after get publisher"() {
        setup:
        def bus = TreeEventBus.newRoot(Dispatchers.immediate())
        def publisher = bus.getPublisher(EventA.class)
        def subscriber = new TestSubscriber()
        bus.subscribe(subscriber)

        when:
        def shouldBeResolve = publisher.callEventA(message)
        def shouldBeMessage = message

        then:
        shouldBeResolve == EventState.RESOLVE
        shouldBeMessage == message

        where:
        message | _
        "test"  | _
    }

    def "call parent event through child publisher"() {
        setup:
        def root = TreeEventBus.newRoot(Dispatchers.immediate())
        def child = root.newChild()
        def rootSubscriber = new TestSubscriber()
        def childSubscriber = new TestSubscriber()
        root.subscribe(rootSubscriber)
        child.subscribe(childSubscriber)

        when:
        def shouldBeResolve = child.getPublisher(EventA.class).callEventA(message)
        def shouldBeMessage = childSubscriber.messageA
        def shouldBeEmpty = rootSubscriber.messageA
        def shouldBePass = child.getPublisher(EventB.class).callEventB(message)
        def shouldBeMessageB = childSubscriber.messageB
        def shouldBeMessageBRoot = rootSubscriber.messageB

        then:
        shouldBeResolve == EventState.RESOLVE
        shouldBeMessage == message
        shouldBeEmpty == ""
        shouldBePass == EventState.PASS
        shouldBeMessageB == message
        shouldBeMessageBRoot == message

        where:
        message | _
        "test"  | _
    }

    def "handle error if OnEventError implemented"(String message) {
        setup:
        def bus = TreeEventBus.newRoot(Dispatchers.immediate())
        def subscriber = new TestSubscriber()
        bus.subscribe(subscriber)

        when:
        def shouldBeResolve = bus.getPublisher(EventD.class).callEventD(message)
        def shouldBeResolved = subscriber.error

        then:
        shouldBeResolve == EventState.RESOLVE
        shouldBeResolved == "resolved"

        where:
        message | _
        "test"  | _
    }

    def "handle child error if OnEventError implemented"() {
        setup:
        def root = TreeEventBus.newRoot(Dispatchers.immediate())
        def child = root.newChild()
        def rootSubscriber = new TestSubscriber()
        def childSubscriber = new ErrorSubscriber()
        root.subscribe(rootSubscriber)
        child.subscribe(childSubscriber)

        when:
        def shouldBeResolve = child.getPublisher(EventA.class).callEventA(message)
        def shouldBeResolved = rootSubscriber.error

        then:
        shouldBeResolve == EventState.RESOLVE
        shouldBeResolved == "resolved"

        where:
        message | _
        "test"  | _
    }

    def "execute no event after eventbus shutdown"() {
        setup:
        def bus = TreeEventBus.newRoot(Dispatchers.immediate())
        def subscriber = new TestSubscriber();
        def publisher = bus.getPublisher(EventA.class)
        bus.subscribe(subscriber)
        bus.shutdown()

        when:
        def shouldBePass = publisher.callEventA(message)
        def shouldBeEmpty = subscriber.messageA

        then:
        shouldBePass == EventState.PASS
        shouldBeEmpty == ""

        where:
        message | _
        "test"  | _
    }

    def "handle no error after eventbus shutdown"() {
        setup:
        def bus = TreeEventBus.newRoot(Dispatchers.immediate())
        def subscriber = new TestSubscriber();
        def publisher = bus.getPublisher(EventD.class)
        bus.subscribe(subscriber)
        bus.shutdown()

        when:
        def shouldBePass = EventState.PASS
        def shouldBeEmpty = subscriber.error

        then:
        shouldBePass == EventState.PASS
        shouldBeEmpty == ""

        where:
        message | _
        "test"  | _
    }

    private static class ErrorSubscriber implements EventA {

        @Override
        EventState callEventA(String message) {
            throw new Error()
        }
    }

    private static class TestSubscriber implements EventA, EventB, EventD, OnEventError {

        def messageA = ""
        def messageB = ""
        def error = ""

        @Override
        EventState callEventA(String message) {
            messageA = message
            return EventState.RESOLVE
        }

        @Override
        EventState callEventB(String message) {
            messageB = message
            return EventState.PASS
        }

        @Override
        EventState callEventD(String message) {
            throw new Error()
        }

        @Override
        EventState onEventError(Throwable e) {
            error = "resolved"
            return EventState.RESOLVE
        }
    }

    @Event
    private interface EventA {
        def EventState callEventA(String message)
    }

    @Event
    private interface EventB {
        def EventState callEventB(String message)
    }

    @Event
    private interface EventC {
        def EventState callEventC(String message)
    }

    @Event
    private interface EventD {
        def EventState callEventD(String message)
    }
}