import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class SensorConsumer {

    private final Consumer<String, String> consumer;
    private static final String COMMAND_TOPIC = "Command";
    private static final String REGISTER_TOPIC = "Register";

    public SensorConsumer() {
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "Sensors");
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");   // DO I NEED TO ACKNOWLEDGE MSGS RECEIPT
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");  // GET ALL MSGS FROM TOPIC
        consumer = new KafkaConsumer<>(consumerProperties);
    }

    public String consumeCommands() {
        consumer.subscribe(Collections.singleton(COMMAND_TOPIC));
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> record : records) {
                return record.value();
            }
        }
    }

}
