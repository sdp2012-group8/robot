package sdp.common;

import java.io.IOException;

/**
 * This is a communicator. It may be used for abstractly passing messages via bluetooth or virtual
 * devices.
 * 
 * @author martinmarinov
 *
 */
public interface Communicator {
	
	/**
	 * Those are the possible command codes
	 * @author martinmarinov
	 *
	 */
	public enum opcode {
		move, exit, moveback, kick, rotate_kicker
	}
	
	/**
	 * Asynchronously send a message to a device.
	 * @param op the opcode {@link opcode}
	 * @param args the arguments
	 * @throws IOException in case of error in the connection
	 */
	public void sendMessage(opcode op, byte... args) throws IOException;
	
	
	public void close();
	

}
