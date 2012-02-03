package sdp.simulator;

import java.io.IOException;
import java.util.ArrayList;

import sdp.common.Communicator;
import sdp.common.MessageListener;

/**
 * 
 * This is a virtual brick - communicator
 * 
 * All units in cm/s
 * 
 * @author s0932707
 *
 */
public class VBrick implements Communicator {
	
	// distance between wheels
	public static final float ROBOTR = 7.1F;
	// radius of wheel
	public static final float WHEELR = 4F;
	// acceleration set on brick
	public static final float ACC = 1000; // acc in degrees/s/s
	// robot size
	public static final float ROBOT_WIDTH = 18; // in cm
	public static final float ROBOT_LENGTH = 20; // in cm
	
	public static final Vector2D front_left = new Vector2D(ROBOT_LENGTH / 2, ROBOT_WIDTH / 2);
	public static final Vector2D front_right = new Vector2D(ROBOT_LENGTH / 2, -ROBOT_WIDTH / 2);
	public static final Vector2D back_left = new Vector2D(-ROBOT_LENGTH / 2, ROBOT_WIDTH / 2);
	public static final Vector2D back_right = new Vector2D(-ROBOT_LENGTH / 2, -ROBOT_WIDTH / 2);
	
	private static final double acceleration = ACC*0.017453292519943295*WHEELR;//69.8;
	private static final double turn_acceleration = ACC*WHEELR/ROBOTR;

	private byte desired_speed = 0,
			desired_turning_speed = 0;
	
	
	private ArrayList<MessageListener> mListener = new ArrayList<MessageListener>();

	@Override
	public void sendMessage(opcode op, byte... args) throws IOException {
		switch (op) {
		case operate:
			desired_speed = args[0];
			desired_turning_speed = args[1];
			break;
		default:
			System.out.print(op+" not recognized by simulator");
			break;
		}
	}

	@Override
	public void close() {

	}

	@Override
	public void registerListener(MessageListener listener) {
		if (!mListener.contains(listener))
			mListener.add(listener);
	}
	
	/**
	 * Calculate speed given old_speed and a time difference
	 * 
	 * This takes into account acceleration settings.
	 * 
	 * @param old_speed the old speed that got calculated dt ago
	 * @param dt time passed since last call of this method
	 * @return the new speed
	 */
	public double calculateSpeed(double old_speed, double dt) {
		// V = Vo + acc*dt
		double speed = old_speed;
		if (!(Math.abs(desired_speed-speed) < acceleration*dt)) {
			if (desired_speed > speed)
				speed+=acceleration*dt;
			else
				speed-=acceleration*dt;
		}
		else
			speed = desired_speed;
		return speed;

	}
	
	/**
	 * Calculate turning speed given old_speed and a time difference.
	 * 
	 * This takes into account acceleration settings.
	 * 
	 * @param old_turning_speed the old turning speed that got calculated dt ago
	 * @param dt time passed since calculation of old_turning_speed
	 * @return the new turning speed
	 */
	public double calculateTurningSpeed(double old_turning_speed, double dt) {
		double turning_speed = old_turning_speed;
		if (!(Math.abs(desired_turning_speed-turning_speed) < turn_acceleration*dt)) {
			if (desired_turning_speed > turning_speed)
				turning_speed+=turn_acceleration*dt;
			else
				turning_speed-=turn_acceleration*dt;
		}
		else
			turning_speed = desired_turning_speed;
		return turning_speed;
	}
	

}
