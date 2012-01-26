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
		move, exit, moveback, kick, rotate_kicker, turn, move_to_wall,
		joypad_forward, // one argument; speed in revolutions per second
		joypad_turn, // one argument; turning speed in degrees per second * 2 (for example 90 means 128 degrees per second)
		joypad_turn_end, // no arguments; when turning stops, return to straight
		play_sound // no arguments
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
