package net.kr9ly.salmon

import net.kr9ly.salmon.dispatcher.Dispatchers
import net.kr9ly.salmon.event.Event
import net.kr9ly.salmon.event.EventState
import spock.lang.Specification

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AsyncDispatcherSpec extends Specification {

    def "dispatch event async"() {
        setup:
        def executorService = Executors.newFixedThreadPool(1)
        def dispatcher = Dispatchers.async(executorService)
        def bus = TreeEventBus.newRoot(dispatcher)
        def subscriber = new TestSubscriber()
        bus.subscribe(subscriber)

        expect:
        bus.getPublisher(EventA.class).callEventA(message) == EventState.RESOLVE
        subscriber.messageA == ""
        executorService.shutdown()
        executorService.awaitTermination(1, TimeUnit.SECONDS)
        subscriber.messageA == message

        where:
        message | _
        "test"  | _
    }

    def "never call event after shutdown executorService"() {
        setup:
        def executorService = Executors.newFixedThreadPool(1)
        def dispatcher = Dispatchers.async(executorService)
        def bus = TreeEventBus.newRoot(dispatcher)
        def subscriber = new TestSubscriber()
        bus.subscribe(subscriber)

        when:
        def shouldBeResolve = bus.getPublisher(EventA.class).callEventA(message)
        def shouldBeEmpty = subscriber.messageA
        executorService.shutdownNow()
        executorService.awaitTermination(1, TimeUnit.SECONDS)
        def shouldBeEmptyAfter = subscriber.messageA

        then:
        shouldBeResolve == EventState.RESOLVE
        shouldBeEmpty == ""
        shouldBeEmptyAfter == ""

        where:
        message | _
        "test"  | _
    }

    private static class TestSubscriber implements EventA {

        def messageA = ""

        @Override
        EventState callEventA(String message) {
            Thread.sleep(100)
            messageA = message
            return EventState.RESOLVE
        }
    }

    @Event
    private interface EventA {
        def EventState callEventA(String message)
    }
}