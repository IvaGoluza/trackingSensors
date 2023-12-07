import network.EmulatedSystemClock;
import network.SimpleSimulatedDatagramSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class UDPclient {

    private static final Logger logger = Logger.getLogger(UDPclient.class.getName());

    public static void sendMsg(int port, String sensorReading) throws IOException {

        byte[] rcvBuf = new byte[256]; // received bytes
        InetAddress address = InetAddress.getByName("localhost");

        DatagramSocket socket = new SimpleSimulatedDatagramSocket(0.3, 1000); //SOCKET
        String msg = sensorReading + "#" + new EmulatedSystemClock().currentTimeMillis();
        byte[] sendBuf = msg.getBytes();

        DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, address, port);
        System.out.println("[UDP client] Sending message to UDP server on port " + port + ". [Message=" + msg + "].");
        socket.send(packet);
        String receivedString = "";

        while (!Sensor.stop) {

            DatagramPacket rcvPacket = new DatagramPacket(rcvBuf, rcvBuf.length); // create a datagram packet for receiving data
            try {
                socket.receive(rcvPacket);  // receive a datagram packet from this socket
                receivedString = new String(rcvPacket.getData(), rcvPacket.getOffset(), rcvPacket.getLength());
                if(receivedString.equals("PACKAGE RECEIVED")) break;
            } catch (SocketTimeoutException e) {
                System.out.println("[UDP client] Message has not been received. Retransmitting.");
                socket.send(packet);        // resend packet
            } catch (IOException ex) {
                Logger.getLogger(SimpleSimulatedDatagramSocket.class.getName()).info(Level.SEVERE + " " + ex);
            }

        }
        System.out.println("[UDP client] Received ACK message from server on port " + port + ". [Message=" + receivedString + "].");
        socket.close(); //CLOSE

    }
}

