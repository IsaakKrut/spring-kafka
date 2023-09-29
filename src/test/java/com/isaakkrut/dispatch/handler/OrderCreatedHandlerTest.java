package com.isaakkrut.dispatch.handler;

import com.isaakkrut.dispatch.exception.NotRetriableException;
import com.isaakkrut.dispatch.exception.RetriableException;
import com.isaakkrut.dispatch.message.OrderCreated;
import com.isaakkrut.dispatch.service.DispatchService;
import com.isaakkrut.dispatch.util.TestEventData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderCreatedHandlerTest {

    private OrderCreatedHandler handler;
    private DispatchService dispatchServiceMock;

    @BeforeEach
    void setUp() {
        dispatchServiceMock = mock(DispatchService.class);
        handler = new OrderCreatedHandler(dispatchServiceMock);
    }

    @Test
    void listen_Success() throws Exception {
        String key = randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        handler.listen(0, key, testEvent);
        verify(dispatchServiceMock, times(1)).process(key, testEvent);
    }

    @Test
    public void listen_ServiceThrowsException() throws Exception {
        String key = randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        doThrow(new RuntimeException("Service failure")).when(dispatchServiceMock).process(key, testEvent);

        Exception exception = assertThrows(NotRetriableException.class, () -> handler.listen(0, key, testEvent));
        assertThat(exception.getMessage(), equalTo("java.lang.RuntimeException: Service failure"));
        verify(dispatchServiceMock, times(1)).process(key, testEvent);
    }

    @Test
    public void testListen_ServiceThrowsRetriableException() throws Exception {
        String key = randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        doThrow(new RetriableException("Service failure")).when(dispatchServiceMock).process(key, testEvent);

        Exception exception = assertThrows(RuntimeException.class, () -> handler.listen(0, key, testEvent));
        assertThat(exception.getMessage(), equalTo("Service failure"));
        verify(dispatchServiceMock, times(1)).process(key, testEvent);
    }
}