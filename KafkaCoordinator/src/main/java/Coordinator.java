import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

public class Coordinator {

    private static final Logger logger = Logger.getLogger(Coordinator.class.getName());


    public static void main(String[] args){

        // producer configuration
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        Scanner scanner = new Scanner(System.in);

        try (Producer<String, String> producer = new KafkaProducer<>(producerProperties)) {   // try-with-resources for producer closing

            while (true) {
                String userInput = scanner.nextLine();
                if (userInput != null) {
                    if (userInput.equalsIgnoreCase("start")) {
                        ProducerRecord<String, String> record = new ProducerRecord<>("Command", null, "start");
                        producer.send(record);
                        logger.info("Kafka coordinator starting system sensors.");
                    } else if (userInput.equalsIgnoreCase("stop")) {
                        ProducerRecord<String, String> record = new ProducerRecord<>("Command", null, "stop");
                        producer.send(record);
                        logger.info("Kafka coordinator stopping system sensors.");
                        scanner.close();
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Could not start producer: " + e);
        }
    }
}
