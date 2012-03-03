package sdp.AI;

import java.awt.geom.Point2D;
import java.io.IOException;

import sdp.common.Communicator;
import sdp.common.MessageQueue;
import sdp.common.Communicator.opcode;
import sdp.common.Utilities;

/**
 * 
 * This is the AI superclass.
 * All AI should extend this class.
 * @author Martin Marinov
 *
 */

public abstract class AI {
	
	// robot constants
	protected final static double TURNING_ACCURACY = 10;
	protected final static double KICKING_ACCURACY = 10;

	protected final static double ROBOT_ACC_CM_S_S = 69.8; // 1000 degrees/s/s
	protected final static int MAX_SPEED_CM_S = 53; // 50 cm per second
	protected final static int MAX_TURNING_SPEED = 127;

	protected AIWorldState ai_world_state= null;
	protected AIWorldState old_ai_world_state = null;
	
	protected abstract Commands chaseBall() throws IOException;
	protected abstract Commands gotBall() throws IOException;
	protected abstract Commands defendGoal() throws IOException;
	protected abstract Commands penaltiesDefend() throws IOException;
	protected abstract Commands penaltiesAttack() throws IOException;
	
	/**
	 * Initialise the AI
	 * 
	 * @param Comm a communicator for making connection with real robot/simulated one
	 * @param Obs an observer for taking information about the table
	 */
	public AI() {
	}
	
	public Commands sit() throws IOException {
		return new Commands(0, 0, false);
	}

	
	
	/**
	 * Updates the held ai_world_state of the AI.
	 * @param ai_world_state
	 */
	protected void update(AIWorldState ai_world_state) {
		if (this.ai_world_state != null) {
			this.old_ai_world_state = this.ai_world_state;
		}
		this.ai_world_state = ai_world_state;
	}
	
	public static byte normaliseSpeed(byte speed) {
		if (speed > MAX_SPEED_CM_S) speed = MAX_SPEED_CM_S;
		if (speed < -MAX_SPEED_CM_S) speed = -MAX_SPEED_CM_S;
		return speed;
	}
	
	public Point2D.Double getOptimalTargetPoint() {
		return this.ai_world_state.getOptimalPointBehindBall();
	}
	
	public static class Commands {
		
		public static final double default_acceleration = 69.81317d;
		
		public double speed, turning_speed, acceleration = default_acceleration;
		public boolean kick;
		
		public Commands(double speed, double turning_speed, boolean kick) {
			this.speed = speed;
			this.turning_speed = turning_speed;
			this.kick = kick;
		}
		
		public byte getByteSpeed() {
			return normaliseSpeed(Utilities.normaliseToByte(speed));
		}
		
		public byte getByteTurnSpeed() {
			return Utilities.normaliseToByte(Utilities.normaliseAngle(turning_speed));
		}
		
		public byte getByteAcc() {
			return Utilities.normaliseToByte(acceleration);
		}
		
		public boolean isDefaultAcc() {
			return acceleration == default_acceleration;
		}
		
		@Override
		public String toString() {
			return "spd: "+getByteSpeed()+", tspd: "+getByteTurnSpeed()+", kick "+kick+", acc: "+getByteAcc();
		}
	}

}
