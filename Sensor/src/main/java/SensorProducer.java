import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.JSONObject;

import java.util.Properties;

public class SensorProducer {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    private static void produce(String id, String address, String port) {
        // producer configuration
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        try (Producer<String, String> producer = new KafkaProducer<>(producerProperties)) {   // try-with-resources for producer closing

            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("address", address);
            json.put("port", port);

            String TOPIC = "Register";
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, null, json.toString());
            producer.send(record);

        } catch (Exception e) {
            System.out.println("Could not start producer: " + e);
        }
    }
}
