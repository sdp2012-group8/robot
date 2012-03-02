package sdp.brick;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lejos.nxt.LCD;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;

import sdp.common.Communicator;
import sdp.common.MessageListener;

/**
 * This class is the handles sending/receiving messages from the PC.
 * 
 * Keep in mind that first the Brick must be running before JControl connects to BControl.
 * 
 * @author martinmarinov
 *
 */
public class BComm implements Communicator {
	
	// private variables
	private static InputStream is;
	private static OutputStream os;
	private static boolean running = false;
	private static int messages_so_far = 0;
	private static NXTConnection connection;
	
	private static final boolean DEBUG = false;
	
	/**
	 * Initialises the controller with a listener
	 * @param listener
	 */
	public BComm() {
		LCD.clear();
		LCD.drawString("Waiting for", 0, 0);
		LCD.drawString("Bluetooth...", 0, 1);
		// itnialize connection
		connection = Bluetooth.waitForConnection();
		LCD.clear(0);
		LCD.clear(1);
		LCD.drawString("Connection",0,0);
		LCD.drawString("established!",0,1);
		is = connection.openInputStream();
		os = connection.openOutputStream();
		running = true;
		// start the listener thread
		
		
	}

	/**
	 * Send a message to the PC
	 * @param op the opcode
	 * @param args the arguments
	 */
	@Override
	public void sendMessage(opcode op, byte... args) throws IOException {
		os.write(op.ordinal()); // write opcode
		os.write(args.length); // write number of args
		os.write(args); // write args
		os.flush(); // send message
	}

	/**
	 * Close the stream and connection gracefully
	 */
	@Override
	public void close() {
		try {
			running = false;
			is.close();
			os.close();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	/**
	 * Only one listener could be registered on brick!
	 */
	@Override
	public void registerListener(final MessageListener listener) {
		new Thread() {

			public void run() {
				while (running) {
						try {
							// wait for an instruction
							opcode op = opcode.values()[is.read()];
							int length = is.read();
							byte[] args = new byte[length];
							for (int i = 0; i < length; i++) {
								args[i] = (byte) is.read();
							}
							// print message on the LCD
							messages_so_far++;
							if (DEBUG) {
							LCD.clear(0);
							LCD.clear(1);
							LCD.clear(2);
							LCD.drawString(messages_so_far+". messgs", 0, 0);
							LCD.drawString(op+";"+length, 0, 1);
							if (args.length > 1) {
								String all = "["+args[0];
								for (int i = 1; i < args.length; i++)
									all += ";"+String.valueOf(args[i]);
								LCD.drawString(all+"]", 0, 2);
							} else if (args.length == 1)
								LCD.drawString("["+args[0]+"]",0, 2);
							else
								LCD.drawString("NO ARGS",0, 2);
							}
							// call the method
							listener.receiveMessage(op, args, BComm.this);
						} catch (IOException e) {
							LCD.clear();
							LCD.drawString("Lost!",0,0);
							e.printStackTrace();
							try {
								sleep(2000);
							} catch (InterruptedException e1) {	}
						}
					}

			};
		}.start();
		
	}

}
