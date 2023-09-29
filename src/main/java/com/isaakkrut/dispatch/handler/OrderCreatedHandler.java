package com.isaakkrut.dispatch.handler;

import com.isaakkrut.dispatch.exception.NotRetriableException;
import com.isaakkrut.dispatch.exception.RetriableException;
import com.isaakkrut.dispatch.message.OrderCreated;
import com.isaakkrut.dispatch.service.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
@KafkaListener(
        id = "orderConsumerClient",
        topics = "order.created",
        groupId = "dispatch.order.created.consumer",
        containerFactory = "kafkaListenerContainerFactory"
)
public class OrderCreatedHandler {


    private final DispatchService dispatchService;

    @KafkaHandler
    public void listen(@Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition, @Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload OrderCreated payload) {
        log.info("Received message: partition: "+partition+" - key: " +key+ " - orderId: " + payload.getOrderId() + " - item: " + payload.getItem());
        try {
            dispatchService.process(key, payload);
        }  catch (RetriableException e) {
            log.warn("Retriable exception: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Not Retriable exception: " + e.getMessage());
            throw new NotRetriableException(e);
        }
    }

}
