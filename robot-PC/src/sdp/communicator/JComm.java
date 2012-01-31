package sdp.communicator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommException;
import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTInfo;
import sdp.common.MessageListener;

/**
 * 
 * This is the interface that will make the connection from the PC to the device
 * 
 * Don't forget to set up the MAC of the brick here!
 * 
 * @author martinmarinov
 *
 */
public class JComm implements sdp.common.Communicator {
	
	// password and mac settings
	private static final int max_retries = 6; // how many times to try connecting to brick before quitting
	private static final String friendly_name = "Ball-E";
	private static final String mac_of_brick = "00:16:53:0A:5C:22";
	
	private final static Logger LOGGER = Logger.getLogger(JComm.class .getName());

	// variables
	private MessageListener mListener;
	private NXTComm mComm;
	private OutputStream os;
    private InputStream is;
    private boolean connection_open = false;
	
	/**
	 * Opens connection with robot.
	 * @param listener the listener that will receive updates from the robot
	 * @throws NXTCommException if a connection cannot be established
	 */
	public JComm(MessageListener listener) throws IOException {
		this.mListener = listener;
		try {
		mComm = NXTCommFactory.createNXTComm(NXTCommFactory.BLUETOOTH);
		} catch (Exception e) {
			LOGGER.warning("Cannot open Bluetooth.");
		}
		NXTInfo info = new NXTInfo(NXTCommFactory.BLUETOOTH,friendly_name, mac_of_brick);
		LOGGER.info("Openning connection...");
		boolean repeat = true;
		int tries = 0;
		while (repeat && tries < max_retries) {
			tries++;
			try {
				mComm.open(info);
				repeat = false;
			} catch (Exception e) {
				LOGGER.warning("Cannot establish connection. Is device on? Retry in 4 s. ("+(max_retries-tries)+" attempts left)");
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e1) {}
			}
		}
		if (repeat)
			throw new IOException("Cannot connect to brick!");
		LOGGER.info("Getting output stream...");
		os = mComm.getOutputStream();
		LOGGER.info("Getting input stream...");
        is = mComm.getInputStream();
        connection_open = true;
        // handle incoming connections in a new thread
        LOGGER.info("Starting listener...");
        new Thread() {
        	public void run() {
        		while (connection_open) {
        			try {
        				readNextMessage();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						LOGGER.warning("Connection with device lost. Waiting 2 s and trying again...");
						e.printStackTrace();
						try {
							sleep(2000);
						} catch (InterruptedException e1) {	}
					}
        		}
        	};
        }.start();
        LOGGER.info("Ready");
	}
	
	/**
	 * Reads the next message from the input stream.
	 * @throws IOException
	 */
	private void readNextMessage() throws IOException {
		int b =is.read();
		if (b == -1)
			throw new IOException("ERROR");
		opcode op = opcode.values()[b];
		System.out.println(op+" arrived");
		int length = is.read();
		byte[] args = new byte[length];
		for (int i = 0; i < length; i++) {
			args[i] = (byte) is.read();
		}
		mListener.receiveMessage(op, args, JComm.this);
	}

	/**
	 * Send a message over bluetooth to the brick
	 * @param op the opcode of the mssage
	 * @param args the arguments
	 */
	@Override
	public void sendMessage(opcode op, byte... args) throws IOException {
		// send a message to device
		os.write(op.ordinal()); // write opcode
		os.write(args.length); // write number of args
		os.write(args); // write args
		os.flush(); // send message
	}

	/**
	 * Gracefully close connection with brick
	 */
	@Override
	public void close() {
		try {
			connection_open = false;
			is.close();
			os.close();
			mComm.close();
		} catch (IOException e) {
			LOGGER.warning("Error while closing connection with brick");
			e.printStackTrace();
		}

	}

}
