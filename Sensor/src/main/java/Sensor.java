import network.EmulatedSystemClock;
import network.UDPMessage;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class Sensor {

    private static final Logger logger = Logger.getLogger(Sensor.class.getName());
    private final String id;

    private final String port;

    private final List<Sensor> systemSensors;

    private final EmulatedSystemClock scalarTime;

    private Map<String, Integer> vectorTime;

    private final List<UDPMessage> readings;

    public static boolean stop = false;

    public Sensor(String id, String port) {
        this.id = id;
        this.port = port;
        this.systemSensors = new ArrayList<>();
        this.scalarTime = new EmulatedSystemClock();
        this.readings = new ArrayList<>();
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

    public EmulatedSystemClock getScalarTime() {
        return scalarTime;
    }

    public void addSystemSensor(Sensor systemSensor) {
        this.systemSensors.add(systemSensor);
    }

    public List<UDPMessage> getReadings() {
        return readings;
    }

    public void addReading(UDPMessage reading) {
        this.readings.add(reading);
    }

    public Map<String, Integer> getVectorTime() {
        return vectorTime;
    }

    public void setVectorTime(Map<String, Integer> vectorTime) {
        this.vectorTime = vectorTime;
    }

    public void updateVectorTime(String id) {
        this.vectorTime.put(id, this.vectorTime.get(id) + 1);
    }

    public void updateVectorTime(Map<String, Integer> newVectorTime) {
        for (Map.Entry<String, Integer> entry : vectorTime.entrySet()) {
            String key = entry.getKey();
            Integer vectorTimeValue = entry.getValue();
            Integer newVectorTimeValue = newVectorTime.get(key);

            if (!Objects.equals(key, this.getId()) && vectorTimeValue < newVectorTimeValue) {
                vectorTime.put(key, newVectorTimeValue);
            }
        }
    }

    public static String generateReading(long activeMilliSeconds, List<CSVRecord> sensorReadings) {
        int row = (int) (((activeMilliSeconds / 1000) % 100) + 1);
        if (row < sensorReadings.size() && row >= 0) {
            CSVRecord currentSensorReading = sensorReadings.get(row);
            return currentSensorReading.get("NO2").isEmpty() ? "0" : currentSensorReading.get("NO2");   // 0.0 or null
        } else return "0";
    }


    public static void orderedPrint(Sensor sensor) {
        List<UDPMessage> last500seconds = sensor.readings.stream().filter(reading -> sensor.getScalarTime().currentTimeMillis() - reading.getScalarTime() <= 5000).toList();
        if (last500seconds.size() == 0) {
            System.out.println("\nNo new readings");
            return;
        }
        System.out.println(" ");
        List<UDPMessage> orderedScalar = new ArrayList<>(last500seconds);
        Comparator<UDPMessage> comparatorScalar = Comparator.comparingLong(UDPMessage::getScalarTime).reversed();
        orderedScalar.sort(comparatorScalar);
        System.out.println("\nLast 5 seconds readings sorted by scalar timestamps.");
        orderedScalar.forEach(reading -> {
            if (reading.getScalarTime() - sensor.getScalarTime().currentTimeMillis() <= 5000) System.out.println(reading);
        });

        List<UDPMessage> orderedVector = new ArrayList<>(last500seconds);
        orderedVector.sort(new VectorTimeComparator());
        System.out.println("\nLast 5 seconds readings sorted by vector timestamps.");
        orderedVector.forEach(reading -> {
            if (reading.getScalarTime() - sensor.getScalarTime().currentTimeMillis() <= 5000) System.out.println(reading);
        });

        // mean
        int readingsSum = 0;
        for (UDPMessage message : last500seconds) {
            readingsSum += Integer.parseInt(message.getReading());
        }
        System.out.println("\nMean: " + readingsSum / last500seconds.size());

    }

    public static void main(String[] args) throws IOException, InterruptedException {

        String csvFilePath = "./src/main/data/readings.csv";    // for csv file reading
        FileReader fileReader = new FileReader(csvFilePath);
        CSVParser csvParser = CSVParser.parse(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrailingDelimiter());
        List<CSVRecord> sensorReadings = csvParser.getRecords();

        Scanner scanner = new Scanner(System.in);   // get sensor info
        System.out.print("Sensor ID: ");
        String id = scanner.nextLine();
        System.out.print("Sensor port: ");
        String port = scanner.nextLine();
        scanner.close();

        Sensor sensor = new Sensor(id, port);    // create sensor object
        logger.info("Sensor Device [ID=" + id + "] [PORT=" + port + "]." );
        Thread consumeThread = new Thread(() -> SensorConsumer.consume(sensor));    // start sensor consumer for command and register topics
        consumeThread.start();

        while (sensor.systemSensors.size() == 0) {     // waiting for rest of the sensors to start UDP communication
           Thread.sleep(2000);
        }
        logger.info("[sensor] All of the sensors have been registered. Starting UDP communication.");

        // generate sensor's vector time
        Map<String, Integer> vectorTime = new HashMap<>();
        vectorTime.put(sensor.getId(), 0);
        sensor.getSystemSensors().forEach(sensor1 -> vectorTime.put(sensor1.getId(), 0));
        sensor.setVectorTime(vectorTime);

        // start sensor's UDP server
        Thread UDPServerThread = new Thread(() -> {
            try {
                UDPServer.receiveMsgs(sensor);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        UDPServerThread.start();

        // start measuring time for readings print
        Thread printThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
                while (!stop) {
                    orderedPrint(sensor);
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        printThread.start();

        while (!stop) {
            // generate current reading
            String currentSensorReading = generateReading(sensor.getScalarTime().currentTimeMillis() - sensor.getScalarTime().getStartTime(), sensorReadings);
            logger.info("[sensor] Sending current NO2 reading: " + currentSensorReading);

            // create message to save
            UDPMessage newMsg = new UDPMessage(currentSensorReading, sensor.getScalarTime().currentTimeMillis(), sensor.getVectorTime());
            sensor.readings.add(newMsg);

            // send current reading to all system sensors in different threads
            ExecutorService executor = Executors.newFixedThreadPool(sensor.systemSensors.size());

            sensor.systemSensors.forEach(systemSensor -> executor.execute(new RunnableClientTask(Integer.parseInt(systemSensor.getPort()), currentSensorReading, sensor)));
            executor.shutdown();
            Thread.sleep(2000);
        }

    }

}
