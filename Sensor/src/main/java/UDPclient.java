import network.UDPMessage;
import network.SimpleSimulatedDatagramSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class UDPClient {

    private static final Logger logger = Logger.getLogger(UDPClient.class.getName());

    public static void sendMsg(int port, String sensorReading, Sensor sensor) throws IOException {

        byte[] rcvBuf = new byte[256]; // received bytes
        InetAddress address = InetAddress.getByName("localhost");

        DatagramSocket socket = new SimpleSimulatedDatagramSocket(0.3, 1000); //SOCKET

        // sensor is sending another reading - another action ...update vectorTime for this sensor
        sensor.updateVectorTime(sensor.getId());

        // create new UDP message -> send current reading with current scalar and vector time
        UDPMessage newReading = new UDPMessage(sensorReading, sensor.getScalarTime().currentTimeMillis(), sensor.getVectorTime());

        String msg = newReading.toString();
        byte[] sendBuf = msg.getBytes();

        DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, address, port);
        logger.info("[UDP client] Sending message to UDP server on port " + port + ". SENDING" + msg + ".");
        socket.send(packet);
        String receivedString;

        while (!Sensor.stop) {

            DatagramPacket rcvPacket = new DatagramPacket(rcvBuf, rcvBuf.length); // create a datagram packet for receiving data
            try {
                socket.receive(rcvPacket);  // receive a datagram packet from this socket
                receivedString = new String(rcvPacket.getData(), rcvPacket.getOffset(), rcvPacket.getLength());
                if(receivedString.contains("ACK")) {                   // sent package has been received -> new action -> update vector time
                    // sensor.updateVectorTime(sensor.getId());
                    logger.info("[UDP client] Received ACK from port " + port + ": " + receivedString + ".");
                    break;
                }
            } catch (SocketTimeoutException e) {
                if(!Sensor.stop) {
                    logger.info("[UDP client] Message " + sensorReading + " has not been received on port " + port + ". Retransmitting.");
                    socket.send(packet);        // resend packet
                } else {
                    break;
                }
            } catch (IOException ex) {
                Logger.getLogger(SimpleSimulatedDatagramSocket.class.getName()).info(Level.SEVERE + " " + ex);
            }

        }
        socket.close(); //CLOSE

    }
}

