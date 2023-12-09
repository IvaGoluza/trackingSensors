import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.JSONObject;

import java.util.Properties;
import java.util.logging.Logger;

public class SensorProducer {

    private static final Logger logger = Logger.getLogger(SensorProducer.class.getName());

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";


    public static void produce(String id, String address, String port) {

        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        try (Producer<String, String> producer = new KafkaProducer<>(producerProperties)) {

            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("address", address);
            json.put("port", port);
            ProducerRecord<String, String> record = new ProducerRecord<>("Register", null, json.toString());
            producer.send(record);
            logger.info("[Producer] Sensor " + id + " registered on topic Register.");

        } catch (Exception e) {
            logger.info("[Producer] Could not start producer: " + e);
        }

    }
}
