import network.EmulatedSystemClock;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Sensor {

    private static final Logger logger = Logger.getLogger(Sensor.class.getName());

    private final String id;

    private final String port;

    private List<Sensor> systemSensors;

    public static boolean stop = false;

    private static final EmulatedSystemClock startTime = new EmulatedSystemClock();

    public Sensor(String id, String port) {
        this.id = id;
        this.port = port;
        this.systemSensors = new ArrayList<>();
        // UDP
    }

    public String getId() {
        return id;
    }

    public String getPort() {
        return port;
    }

    public List<Sensor> getSystemSensors() {
        return systemSensors;
    }

    public void addSystemSensor(Sensor systemSensor) {
        this.systemSensors.add(systemSensor);
    }

    public static String generateReading(long activeMiliSeconds, List<CSVRecord> sensorReadings) {
        int row = (int) (((activeMiliSeconds / 1000) % 100) + 1);
        if (row < sensorReadings.size()) {
            CSVRecord currentSensorReading = sensorReadings.get(row);
            return currentSensorReading.get("NO2").isEmpty() ? "0" : currentSensorReading.get("NO2");   // 0.0 or null
        } else return null;
    }

    public static void main(String[] args) throws IOException, InterruptedException {


        String csvFilePath = "./src/main/data/readings.csv";    // for csv file reading
        FileReader fileReader = new FileReader(csvFilePath);
        CSVParser csvParser = CSVParser.parse(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrailingDelimiter());
        List<CSVRecord> sensorReadings = csvParser.getRecords();

        Scanner scanner = new Scanner(System.in);   // 0. get sensor info
        System.out.print("Sensor ID: ");
        String id = scanner.nextLine();
        System.out.print("Sensor port: ");
        String port = scanner.nextLine();
        scanner.close();

        Sensor sensor = new Sensor(id, port);    // 1. create sensor object
        System.out.println("Sensor Device [ID=" + id + "] [PORT=" + port + "]." );
        Thread consumeThread = new Thread(() -> SensorConsumer.consume(sensor));    // 2. start sensor consumer for command and register topics
        consumeThread.start();

        while (sensor.systemSensors.size() == 0) {     // 3. waiting for rest of the sensors to start UDP communication
           Thread.sleep(2000);
        }

        System.out.println("[sensor] All of the sensors have been registered. Starting UDP communication.");
        // start sensor's UDP server
        Thread UDPServerThread = new Thread(() -> {
            try {
                UDPserver.receiveMsgs(sensor.getPort());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        UDPServerThread.start();

        while (!stop) {
            // 4. generate current reading
            String currentSensorReading = generateReading(startTime.currentTimeMillis() - startTime.getStartTime(), sensorReadings);
            System.out.println("[sensor] Sending current NO2 reading: " + currentSensorReading);

            //  5. send current reading to all system sensors in different threads
            ExecutorService executor = Executors.newFixedThreadPool(sensor.systemSensors.size());

            sensor.systemSensors.forEach(systemSensor -> {
                executor.execute(new RunnableClientTask(Integer.parseInt(systemSensor.getPort()), currentSensorReading));
            });
            executor.shutdown();
            Thread.sleep(2000);
        }



    }


}
