package sdp.AI;

import java.awt.geom.Point2D;
import java.io.IOException;

import sdp.common.Communicator;
import sdp.common.MessageQueue;
import sdp.common.Communicator.opcode;

/**
 * 
 * This is the AI superclass.
 * All AI should extend this class.
 * @author Martin Marinov
 *
 */

public abstract class AI {
	
	// robot constants
	protected final static double TURNING_ACCURACY = 5;

	protected final static double ROBOT_ACC_CM_S_S = 69.8; // 1000 degrees/s/s
	protected final static int MAX_SPEED_CM_S = 30; // 50 cm per second
	protected final static int MAX_TURNING_SPEED = 50;

	protected AIWorldState ai_world_state= null;
	protected AIWorldState old_ai_world_state = null;
	private MessageQueue mQueue = null;
	protected Communicator mComm = null;
	

	
	protected abstract void chaseBall() throws IOException;
	protected abstract void gotBall() throws IOException;
	
	/**
	 * Initialise the AI
	 * 
	 * @param Comm a communicator for making connection with real robot/simulated one
	 * @param Obs an observer for taking information about the table
	 */
	public AI(Communicator Comm) {
		mQueue = new MessageQueue(Comm);
		this.mComm = Comm;
	}

	
	
	public void sit() throws IOException {
		mComm.sendMessage(opcode.operate, (byte)0, (byte)0);
	}


	/**
	 * Gracefully close AI
	 */
	public void close() {
		// disconnect queue
		mQueue.addMessageToQueue(0, opcode.exit);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// close queue
		mQueue.close();
	}

	/**
	 * Gets the angle between two points
	 * @param A
	 * @param B
	 * @return if you stand at A how many degrees should you turn to face B
	 */
	protected double anglebetween(Point2D.Double A, Point2D.Double B) {
		return (180*Math.atan2(-B.getY()+A.getY(), B.getX()-A.getX()))/Math.PI;
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

}
