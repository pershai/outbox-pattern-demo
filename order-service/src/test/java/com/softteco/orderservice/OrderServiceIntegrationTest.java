package com.softteco.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softteco.orderservice.contracts.OrderCreatedEvent;
import com.softteco.orderservice.domain.OrderEntity;
import com.softteco.orderservice.domain.OrderRepository;
// Import from shared outbox-commons library
import com.softteco.outbox.entity.OutboxEventEntity;
import com.softteco.outbox.repository.OutboxEventRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OrderServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateOrderAndPublishEvent() throws Exception {
        // Given
        String orderJson = """
                {
                    "productSku": "TEST-SKU",
                    "quantity": 2,
                    "amount": 100.00,
                    "customerEmail": "test@example.com"
                }
                """;

        // Setup Kafka Consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList("orders.created.v1"));

            // When
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(orderJson))
                    .andExpect(status().isCreated());

            // Then
            // 1. Verify Order in DB
            List<OrderEntity> orders = orderRepository.findAll();
            assertThat(orders).hasSize(1);
            OrderEntity order = orders.get(0);
            assertThat(order.getProductSku()).isEqualTo("TEST-SKU");

            // 2. Verify Outbox Event in DB
            List<OutboxEventEntity> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
            assertThat(outboxEvents.get(0).getEventType()).isEqualTo("orders.created");

            // 3. Verify Kafka Message (wait for Outbox Relay to publish)
            boolean messageReceived = false;
            for (int i = 0; i < 10; i++) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                if (!records.isEmpty()) {
                    ConsumerRecord<String, String> record = records.iterator().next();
                    String value = record.value();
                    OrderCreatedEvent event = objectMapper.readValue(value, OrderCreatedEvent.class);
                    assertThat(event.productSku()).isEqualTo("TEST-SKU");
                    assertThat(event.quantity()).isEqualTo(2);
                    assertThat(event.amount()).isEqualTo(100.00);
                    assertThat(event.customerEmail()).isEqualTo("test@example.com");
                    messageReceived = true;
                    break;
                }
            }
            assertThat(messageReceived)
                    .withFailMessage("Expected Kafka message on topic 'orders.created.v1' but none received")
                    .isTrue();
        }
    }

    @Test
    void shouldRejectInvalidOrder_MissingEmail() throws Exception {
        // Given
        String invalidOrderJson = """
                {
                    "productSku": "TEST-SKU",
                    "quantity": 2,
                    "amount": 100.00
                }
                """;

        // When/Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidOrderJson))
                .andExpect(status().isBadRequest());

        // Verify no order or outbox event was created
        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectInvalidOrder_NegativeAmount() throws Exception {
        // Given
        String invalidOrderJson = """
                {
                    "productSku": "TEST-SKU",
                    "quantity": 2,
                    "amount": -10.00,
                    "customerEmail": "test@example.com"
                }
                """;

        // When/Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidOrderJson))
                .andExpect(status().isBadRequest());

        // Verify no order or outbox event was created
        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectInvalidOrder_InvalidEmailFormat() throws Exception {
        // Given
        String invalidOrderJson = """
                {
                    "productSku": "TEST-SKU",
                    "quantity": 2,
                    "amount": 100.00,
                    "customerEmail": "not-a-valid-email"
                }
                """;

        // When/Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidOrderJson))
                .andExpect(status().isBadRequest());

        // Verify no order or outbox event was created
        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void shouldVerifyOutboxEventStructure() throws Exception {
        // Given
        String orderJson = """
                {
                    "productSku": "OUTBOX-TEST",
                    "quantity": 5,
                    "amount": 250.00,
                    "customerEmail": "outbox@example.com"
                }
                """;

        // When
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isCreated());

        // Then
        List<OutboxEventEntity> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(1);

        OutboxEventEntity event = outboxEvents.get(0);
        assertThat(event.getEventType()).isEqualTo("orders.created");

        assertThat(event.getStatus()).isEqualTo(com.softteco.outbox.entity.OutboxStatus.NEW);
        assertThat(event.getPayload()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getEventId()).isNotNull();

        // Verify payload contains order data
        assertThat(event.getPayload()).contains("OUTBOX-TEST");
        assertThat(event.getPayload()).contains("outbox@example.com");
    }

    @Test
    void shouldHandleMultipleOrdersSequentially() throws Exception {
        // Given
        String order1Json = """
                {
                    "productSku": "SKU-001",
                    "quantity": 1,
                    "amount": 50.00,
                    "customerEmail": "customer1@example.com"
                }
                """;

        String order2Json = """
                {
                    "productSku": "SKU-002",
                    "quantity": 3,
                    "amount": 150.00,
                    "customerEmail": "customer2@example.com"
                }
                """;

        // When
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(order1Json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(order2Json))
                .andExpect(status().isCreated());

        // Then
        List<OrderEntity> orders = orderRepository.findAll();
        assertThat(orders).hasSize(2);

        List<OutboxEventEntity> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(2);

        // Verify each order has its own outbox event
        assertThat(orders.stream().map(OrderEntity::getProductSku))
                .containsExactlyInAnyOrder("SKU-001", "SKU-002");
    }
}
