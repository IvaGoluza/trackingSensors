import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class Coordinator {

    private static void produce() {
        // producer configuration
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        try (Producer<String, String> producer = new KafkaProducer<>(producerProperties)) {   // try-with-resources for producer closing

            while (true) {
                String TOPIC = "Command";
                ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, null, "start");
                producer.send(record);
                try {
                    Thread.sleep(120000);  // let the sensors work for 2 minutes
                } catch (InterruptedException e) {
                    break;
                }
                record = new ProducerRecord<>(TOPIC, null, "stop");
                producer.send(record);
            }

        } catch (Exception e) {
            System.out.println("Could not start producer: " + e);
        }
    }

    public static void main(String[] args){

        produce();

    }

}
