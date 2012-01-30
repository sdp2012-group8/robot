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
<<<<<<< HEAD
		move, exit, moveback, kick, rotate_kicker, printmsg,moveangle
=======
		move, exit, moveback, kick, rotate_kicker, turn, move_to_wall, checkTouch,
		operate, // two arguments; 
				 // speed in cm per second
				 // turning speed of robot in degrees per second
		play_sound // no arguments
>>>>>>> b96687b875cf2d04bb4f5823a5b6b7b6fd42ea74
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
