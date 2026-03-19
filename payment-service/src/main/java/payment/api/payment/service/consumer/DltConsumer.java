package payment.api.payment.service.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DltConsumer {
    @KafkaListener(
            topics = "orders.DLT",
            groupId = "payment-service-dlt-group"
    )
    public void handleDlt(ConsumerRecord<String,String> record){
        String originalTopic = getStringHeader(record,"kafka_dlt-original-topic");
        String originalPartition = getIntHeader(record, "kafka_dlt-original-partition");
        String originalOffset = getLongHeader(record, "kafka_dlt-original-offset");
        String exceptionMessage = getStringHeader(record, "kafka_dlt-exception-message");
        String consumerGroup = getStringHeader(record, "kafka_dlt-original-consumer-group");

        log.error("""
                Dead letter Message Received
                Original topic: {}
                Original Partition: {}
                Original Offset: {}
                Consumer group: {}
                Failure reason: {}
                Payload: {}
                """,
                originalTopic,
                originalPartition,
                originalOffset,
                consumerGroup,
                exceptionMessage,
                record.value());

    }

    private String getStringHeader(ConsumerRecord<?,?> record, String headerKey){
        Header header = record.headers().lastHeader(headerKey);
        if(header == null || header.value() == null) return "unknow";
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private String getIntHeader(ConsumerRecord<?,?> record, String headerKey){
        Header header = record.headers().lastHeader(headerKey);
        if (header == null || header.value() == null) return "unknow";
        return String.valueOf(ByteBuffer.wrap(header.value()).getInt());
    }

    private String getLongHeader(ConsumerRecord<?,?> record, String headerKey){
        Header header = record.headers().lastHeader(headerKey);
        if (header == null || header.value() == null) return "unknow";
        return String.valueOf(ByteBuffer.wrap(header.value()).getLong());
    }
}
