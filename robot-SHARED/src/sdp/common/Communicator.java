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
		exit, kick, move_to_wall, 
		operate, // two arguments; 
				 // speed in cm per second
				 // turning speed of robot in degrees per second
		play_sound // no arguments
		, rotate_kicker_stop,float_kicker,
		sensor_left, sensor_right, battery, sensor_dist
	}
	
	public enum sensorType {
		front_sonar
	}
		
	/**
	 * Asynchronously send a message to a device.
	 * @param op the opcode {@link opcode}
	 * @param args the arguments
	 * @throws IOException in case of error in the connection
	 */
	public void sendMessage(opcode op, byte... args) throws IOException;
	
	/**
	 * Registers a message listener
	 * @param listener
	 */
	public void registerListener(MessageListener listener);
	
	
	public void close();
	

}
