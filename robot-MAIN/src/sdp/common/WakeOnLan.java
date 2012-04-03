package sdp.common;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class WakeOnLan {
	
	public static void main(String[] args) {
		try {
			System.out.println(InetAddress.getByName("hordichuk").getHostAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		wakeOnLan("hordichuk");
	}
    
    public static final int PORT = 9;    
    
    public static void wakeOnLan(String host) {
        
    	String ipStr = host;
       
     
        try {
        	InetAddress address = InetAddress.getByName(ipStr);
        	
        
            String macStr = getMac(address);
        	
            byte[] macBytes = getMacBytes(macStr);
            byte[] bytes = new byte[6 + 16 * macBytes.length];
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) 0xff;
            }
            for (int i = 6; i < bytes.length; i += macBytes.length) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
            }
            
            
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, PORT);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();
            
            System.out.println("Wake-on-LAN packet sent.");
        }
        catch (Exception e) {
            System.out.println("Failed to send Wake-on-LAN packet: + e");
            System.exit(1);
        }
        
    }
    
    private static byte[] getMacBytes(String macStr) throws IllegalArgumentException {
        byte[] bytes = new byte[6];
        String[] hex = macStr.split("(\\:|\\-)");
        if (hex.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }
        return bytes;
    }
    
    private static String getMac(InetAddress address) {
        try {
            //InetAddress address = InetAddress.getLocalHost();

            /*
             * Get NetworkInterface for the current host and then read
             * the hardware address.
             */
            NetworkInterface ni = 
                    NetworkInterface.getByInetAddress(address);
            if (ni != null) {
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    /*
                     * Extract each array of mac address and convert it 
                     * to hexa with the following format 
                     * 08-00-27-DC-4A-9E.
                     */
                	String macAdd = "";
                    for (int i = 0; i < mac.length; i++) {
                        macAdd+=String.format("%02X%s",
                                mac[i], (i < mac.length - 1) ? "-" : "");
                    }
                    return macAdd;
                } else {
                    System.out.println("Address doesn't exist or is not " +
                            "accessible.");
                }
            } else {
                System.out.println("Network Interface for the specified " +
                        "address is not found.");
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return null;
    }
   
}