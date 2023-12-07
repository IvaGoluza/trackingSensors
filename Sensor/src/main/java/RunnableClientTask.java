import java.io.IOException;

public class RunnableClientTask implements Runnable {
    private int port;
    private String sensorReading;


    public RunnableClientTask(int port, String sensorReading) {
        this.port = port;
        this.sensorReading = sensorReading;
    }

    @Override
    public void run() {
        try {
            UDPclient.sendMsg(port, sensorReading);
        } catch ( IOException e) {
            e.printStackTrace();
        }
    }
}


