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
	
	
	private static final double acceleration = ACC*0.017453292519943295*WHEELR;//69.8;
	private static final double turn_acceleration = ACC*WHEELR/ROBOTR;
	
	private double speed = 0,
			turning_speed = 0,
			direction = 0;
	private byte desired_speed = 0,
			desired_turning_speed = 0;
	
	private Vector2D velocity = Vector2D.ZERO;
	
	
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
	 * Calculate and get the velocity of the robot taking
	 * care of acceleration, etc
	 * 
	 * @param dt time passed since last call of this method
	 * @return
	 */
	public Vector2D getRobotVelocity(double dt) {
		// V = Vo + acc*dt
		if (!(Math.abs(desired_speed-speed) < acceleration*dt)) {
			if (desired_speed > speed)
				speed+=acceleration*dt;
			else
				speed-=acceleration*dt;
		}
		else
			speed = desired_speed;
		if (!(Math.abs(desired_turning_speed-turning_speed) < turn_acceleration*dt)) {
			if (desired_turning_speed > turning_speed)
				turning_speed+=turn_acceleration*dt;
			else
				turning_speed-=turn_acceleration*dt;
		}
		else
			turning_speed = desired_turning_speed;
		direction+=turning_speed*dt;
		velocity.setX(speed*Math.cos(direction*Math.PI/180));
		velocity.setY(speed*Math.sin(direction*Math.PI/180));
		return velocity;
	}
	
	/** 
	 * Call this after {@link #getRobotVelocity(double)}
	 * 
	 */
	public double getDirection() {
		return direction;
	}

}
