package com.isaakkrut.dispatch.service;

import com.isaakkrut.dispatch.client.StockServiceClient;
import com.isaakkrut.dispatch.message.DispatchCompleted;
import com.isaakkrut.dispatch.message.DispatchPreparing;
import com.isaakkrut.dispatch.message.OrderCreated;
import com.isaakkrut.dispatch.message.OrderDispatched;
import com.isaakkrut.dispatch.util.TestEventData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DispatchServiceTest {

    private DispatchService service;
    private KafkaTemplate kafkaProducerMock;
    private StockServiceClient stockServiceClientMock;

    @BeforeEach
    void setUp() {
        kafkaProducerMock = mock(KafkaTemplate.class);
        stockServiceClientMock = mock(StockServiceClient.class);
        service = new DispatchService(kafkaProducerMock, stockServiceClientMock);
    }

    @Test
    void process_Success() throws Exception {
        when(kafkaProducerMock.send(anyString(), anyString(), any(DispatchPreparing.class))).thenReturn(mock(CompletableFuture.class));
        when(kafkaProducerMock.send(anyString(), anyString(), any(OrderDispatched.class))).thenReturn(mock(CompletableFuture.class));
        when(kafkaProducerMock.send(anyString(), anyString(), any(DispatchCompleted.class))).thenReturn(mock(CompletableFuture.class));
        when(stockServiceClientMock.checkAvailability(anyString())).thenReturn("true");

        String key = randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        service.process(key, testEvent);

        verify(kafkaProducerMock, times(1)).send(eq("dispatch.tracking"), eq(key), any(DispatchPreparing.class));
        verify(kafkaProducerMock, times(1)).send(eq("order.dispatched"), eq(key), any(OrderDispatched.class));
        verify(kafkaProducerMock, times(1)).send(eq("dispatch.tracking"), eq(key), any(DispatchCompleted.class));
        verify(stockServiceClientMock, times(1)).checkAvailability(testEvent.getItem());
    }

    @Test
    public void testProcess_StockUnavailable() throws Exception {
        when(stockServiceClientMock.checkAvailability(anyString())).thenReturn("false");

        String key = randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        service.process(key, testEvent);
        verifyNoInteractions(kafkaProducerMock);
        verify(stockServiceClientMock, times(1)).checkAvailability(testEvent.getItem());
    }

    @Test
    public void testProcess_DispatchTrackingProducerThrowsException() {
        String key = randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        when(stockServiceClientMock.checkAvailability(anyString())).thenReturn("true");
        doThrow(new RuntimeException("dispatch tracking producer failure")).when(kafkaProducerMock).send(eq("dispatch.tracking"), eq(key), any(DispatchPreparing.class));

        Exception exception = assertThrows(RuntimeException.class, () -> service.process(key, testEvent));

        verify(kafkaProducerMock, times(1)).send(eq("dispatch.tracking"), eq(key), any(DispatchPreparing.class));
        verifyNoMoreInteractions(kafkaProducerMock);
        verify(stockServiceClientMock, times(1)).checkAvailability(testEvent.getItem());
        assertThat(exception.getMessage(), equalTo("dispatch tracking producer failure"));
    }

    @Test
    public void testProcess_OrderDispatchedProducerThrowsException() {
        String key = randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        when(kafkaProducerMock.send(anyString(), anyString(), any(DispatchPreparing.class))).thenReturn(mock(CompletableFuture.class));
        when(stockServiceClientMock.checkAvailability(anyString())).thenReturn("true");
        doThrow(new RuntimeException("order dispatched producer failure")).when(kafkaProducerMock).send(eq("order.dispatched"), eq(key), any(OrderDispatched.class));

        Exception exception = assertThrows(RuntimeException.class, () -> service.process(key, testEvent));

        verify(kafkaProducerMock, times(1)).send(eq("dispatch.tracking"), eq(key), any(DispatchPreparing.class));
        verify(kafkaProducerMock, times(1)).send(eq("order.dispatched"), eq(key), any(OrderDispatched.class));
        verifyNoMoreInteractions(kafkaProducerMock);
        verify(stockServiceClientMock, times(1)).checkAvailability(testEvent.getItem());
        assertThat(exception.getMessage(), equalTo("order dispatched producer failure"));
    }

    @Test
    public void testProcess_SecondDispatchTrackingProducerThrowsException() {
        when(kafkaProducerMock.send(anyString(), anyString(), any(DispatchPreparing.class))).thenReturn(mock(CompletableFuture.class));
        when(kafkaProducerMock.send(anyString(), anyString(), any(OrderDispatched.class))).thenReturn(mock(CompletableFuture.class));
        when(stockServiceClientMock.checkAvailability(anyString())).thenReturn("true");

        String key = randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());
        doThrow(new RuntimeException("dispatch tracking producer failure")).when(kafkaProducerMock).send(eq("dispatch.tracking"), eq(key), any(DispatchCompleted.class));

        Exception exception = assertThrows(RuntimeException.class, () -> service.process(key, testEvent));

        verify(kafkaProducerMock, times(1)).send(eq("dispatch.tracking"), eq(key), any(DispatchPreparing.class));
        verify(kafkaProducerMock, times(1)).send(eq("order.dispatched"), eq(key), any(OrderDispatched.class));
        verify(kafkaProducerMock, times(1)).send(eq("dispatch.tracking"), eq(key), any(DispatchCompleted.class));
        verify(stockServiceClientMock, times(1)).checkAvailability(testEvent.getItem());
        assertThat(exception.getMessage(), equalTo("dispatch tracking producer failure"));
    }

    @Test
    public void testProcess_StockServiceClient_ThrowsException() {
        String key = randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(randomUUID(), randomUUID().toString());

        doThrow(new RuntimeException("stock service client failure")).when(stockServiceClientMock).checkAvailability(testEvent.getItem());

        Exception exception = assertThrows(RuntimeException.class, () -> service.process(key, testEvent));
        assertThat(exception.getMessage(), equalTo("stock service client failure"));

        verifyNoInteractions(kafkaProducerMock);
        verify(stockServiceClientMock, times(1)).checkAvailability(testEvent.getItem());
    }
}