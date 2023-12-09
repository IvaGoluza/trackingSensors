import java.io.IOException;

public class RunnableClientTask implements Runnable {
    private final int port;
    private final String sensorReading;
    private final Sensor sensor;

    public RunnableClientTask(int port, String sensorReading, Sensor sensor) {
        this.port = port;
        this.sensorReading = sensorReading;
        this.sensor = sensor;
    }

    @Override
    public void run() {
        try {
            UDPClient.sendMsg(port, sensorReading, sensor);
        } catch ( IOException e) {
            e.printStackTrace();
        }
    }
}


