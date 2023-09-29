package com.isaakkrut.dispatch.service;

import com.isaakkrut.dispatch.client.StockServiceClient;
import com.isaakkrut.dispatch.message.DispatchCompleted;
import com.isaakkrut.dispatch.message.DispatchPreparing;
import com.isaakkrut.dispatch.message.OrderCreated;
import com.isaakkrut.dispatch.message.OrderDispatched;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.util.UUID.randomUUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {


    private static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";
    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private static final UUID APPLICATION_ID = randomUUID();
    private final KafkaTemplate<String, Object> kafkaProducer;
    private final StockServiceClient stockServiceClient;

    public void process(String key, OrderCreated orderCreated) throws Exception {

        String available = stockServiceClient.checkAvailability(orderCreated.getItem());

        if(Boolean.valueOf(available)) {
            DispatchPreparing dispatchPreparing = DispatchPreparing.builder()
                    .orderId(orderCreated.getOrderId())
                    .build();
            kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchPreparing).get();

            OrderDispatched orderDispatched = OrderDispatched.builder()
                    .orderId(orderCreated.getOrderId())
                    .processedById(APPLICATION_ID)
                    .notes("Dispatched: " + orderCreated.getItem())
                    .build();
            kafkaProducer.send(ORDER_DISPATCHED_TOPIC, key, orderDispatched).get();

            DispatchCompleted dispatchCompleted = DispatchCompleted.builder()
                    .orderId(orderCreated.getOrderId())
                    .dispatchedDate(LocalDate.now().toString())
                    .build();
            kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchCompleted).get();

            log.info("Sent messages: key: " + key + " - orderId: " + orderCreated.getOrderId() + " - processedById: " + APPLICATION_ID);
        } else {
            log.info("Item " + orderCreated.getItem() + " is unavailable.");
        }
    }
}
