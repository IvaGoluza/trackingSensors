import network.SimpleSimulatedDatagramSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;


public class UDPserver {

    private static final Logger logger = Logger.getLogger(UDPserver.class.getName());

    public static void receiveMsgs(String port) throws IOException {

        List<String> receivedReadings = new ArrayList<>();
        byte[] rcvBuf = new byte[256];  // received bytes
        byte[] sendBuf; // sent bytes
        String sensorReading;

        DatagramSocket socket = new SimpleSimulatedDatagramSocket(Integer.parseInt(port), 0.3, 1000);

        while (!Sensor.stop) {

            DatagramPacket packet = new DatagramPacket(rcvBuf, rcvBuf.length); // create a DatagramPacket for receiving packets
            socket.receive(packet);

            sensorReading = new String(packet.getData(), packet.getOffset(), packet.getLength());
            System.out.println("[UDP server] Received another system's sensor reading: " + sensorReading + ". Sending back ACK message.");

            // check if this package has already been received and reading saved
            String finalSensorReading = sensorReading;
            if (receivedReadings.stream().noneMatch(reading -> Objects.equals(reading, finalSensorReading))) {
                receivedReadings.add(sensorReading);                      // add a new reading
            }

            sendBuf = ("PACKAGE RECEIVED").getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, packet.getAddress(), packet.getPort());
            socket.send(sendPacket);
        }
        System.out.println("[UDP server] Stopping UDP communication between sensors. Listing readings.");
        receivedReadings.forEach(System.out::println);
        socket.close();
    }
}

