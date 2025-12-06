package com.softteco.stockservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softteco.stockservice.service.StockReservationService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = { "orders.created.v1", "orders.created.v1.DLT" })
class StockServiceDlqTest {

        @Autowired
        private EmbeddedKafkaBroker embeddedKafkaBroker;

        @Autowired
        private KafkaTemplate<String, Object> kafkaTemplate;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private StockReservationService stockReservationService;

        @Test
        void testDlq() throws Exception {
                // Given
                doThrow(new RuntimeException("Simulated failure"))
                                .when(stockReservationService).handleOrder(any());
                Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true",
                                embeddedKafkaBroker);
                consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(consumerProps,
                                new StringDeserializer(),
                                new StringDeserializer())
                                .createConsumer();
                consumer.subscribe(Collections.singleton("orders.created.v1.DLT"));

                ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer,
                                "orders.created.v1.DLT");
                assertThat(record).isNotNull();
                assertThat(record.value()).contains("FAIL-ME");
        }
}
