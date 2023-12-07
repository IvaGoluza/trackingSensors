import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONObject;

import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

public class SensorConsumer {

    private static final Logger logger = Logger.getLogger(SensorConsumer.class.getName());

    public static void consume(Sensor sensor) {

        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, sensor.getId());
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");  // ACKNOWLEDGE MSGS RECEIPT
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");  // GET ALL MSGS FROM TOPIC

        try (Consumer<String, String> consumer = new KafkaConsumer<>(consumerProperties)) {
            consumer.subscribe(Arrays.asList("Register", "Command"));
            System.out.println("[Consumer] Sensor " + sensor.getId() + " subscribed to topics: Command, Register.");

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                records.forEach(record -> {
                    if (Objects.equals(record.topic(), "Command")) {
                        handleCommand(record.value(), sensor);
                    } else if (Objects.equals(record.topic(), "Register")) {
                        handleRegister(record.value(), sensor);
                    }

                });
                consumer.commitAsync();
            }
        }

    }

    public static void handleCommand(String command, Sensor sensor) {
        if (Objects.equals(command, "start")) {    // If sensor has been started -> sensor registration via sensor producer
            System.out.println("[Consumer] Sensor " + sensor.getId() + " got START command from Kafka coordinator.");
            SensorProducer.produce(sensor.getId(), "localhost", sensor.getPort());
        } else if (Objects.equals(command, "stop")) {
            System.out.println("[Consumer] Sensor " + sensor.getId() + " got STOP command from Kafka coordinator.");
            Sensor.stop = true;
        }

    }

    public static void handleRegister(String sensorInfo, Sensor sensor) {

        JSONObject json = new JSONObject(sensorInfo);
        String id = json.getString("id");
        String port = json.getString("port");
        if (!Objects.equals(id, sensor.getId())) {
            Sensor newSensor = new Sensor(id, port);
            sensor.addSystemSensor(newSensor);
            System.out.println("[Consumer] New system sensor registered on Register topic: [ID=" + id + "] [PORT=" + port + "].");
        }

    }

}
