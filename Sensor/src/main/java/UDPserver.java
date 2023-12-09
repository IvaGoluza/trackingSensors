import network.UDPMessage;
import network.SimpleSimulatedDatagramSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;
import java.util.logging.Logger;


public class UDPServer {

    private static final Logger logger = Logger.getLogger(UDPServer.class.getName());

    public static UDPMessage createUDPMessage(String messageString) {

        String[] parts = messageString.replaceAll("[\\[\\]]", "").split(", ");
        String reading = parts[0].split("=")[1];
        long scalarTime = Long.parseLong(parts[1].split("=")[1]);

        String[] vectorParts = messageString.replaceAll("[\\[\\]]", "").split("vectorTime=")[1]
                .replaceAll("\\{", "")
                .replaceAll("\\}", "")
                .split(", ");

        Map<String, Integer> vectorTime = new HashMap<>();
        for (String vectorPart : vectorParts) {
            String[] entry = vectorPart.split("=");
            vectorTime.put(entry[0], Integer.parseInt(entry[1]));
        }

        return new UDPMessage(reading, scalarTime, vectorTime);
    }

    public static void receiveMsgs(Sensor sensor) throws IOException {

        byte[] rcvBuf = new byte[256];  // received bytes
        byte[] sendBuf; // sent bytes
        String sensorReading;

        DatagramSocket socket = new SimpleSimulatedDatagramSocket(Integer.parseInt(sensor.getPort()), 0.3, 1000);

        while (!Sensor.stop) {

            DatagramPacket packet = new DatagramPacket(rcvBuf, rcvBuf.length); // create a DatagramPacket for receiving packets
            socket.receive(packet);

            sensorReading = new String(packet.getData(), packet.getOffset(), packet.getLength());
            logger.info("[UDP server] Received another system sensor reading: RECEIVED" + sensorReading + ". Sending back ACK message.");

            // check if this package has already been received and reading saved
            UDPMessage newReadingMsg = createUDPMessage(sensorReading);
            if (sensor.getReadings().stream().noneMatch(reading -> reading.equals(newReadingMsg))) {
                sensor.addReading(newReadingMsg);                      // add a new reading
                // update sensor's scalar time if it's earlier than new reading's scalar time
                if (newReadingMsg.getScalarTime() < sensor.getScalarTime().currentTimeMillis()) {
                    sensor.getScalarTime().setStartTime(newReadingMsg.getScalarTime());
                }

                // update sensor's vector time
                sensor.updateVectorTime(sensor.getId());  // update value of this sensor
                sensor.updateVectorTime(newReadingMsg.getVectorTime()); // update for the rest of the system sensors
            }

            // send ACK message
            sendBuf = ("ACK " + newReadingMsg.getReading()).getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, packet.getAddress(), packet.getPort());
            socket.send(sendPacket);
        }
        logger.info("[UDP server] Stopping UDP communication between sensors. Listing readings.");
        socket.close();
    }
}

