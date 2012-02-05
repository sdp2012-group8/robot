package sdp.AI;

import java.awt.geom.Point2D;

import sdp.common.Communicator;
import sdp.common.MessageQueue;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;
import sdp.common.Robot;
import sdp.common.Communicator.opcode;
import sdp.common.WorldStateProvider;

/**
 * 
 * This is the AI class that will take decisions.
 * 
 * @author Martin Marinov
 *
 */

public abstract class AI extends WorldStateProvider {
	
	public enum mode {
		chase_once, chase_ball, sit, got_ball
	}

	// pitch constants
	private final static double PITCH_WIDTH_CM = 244;
	private final static double GOAL_Y_CM = 113.7/2;
	// robot constants
	protected final static double TURNING_ACCURACY = 10;

	protected final static double ROBOT_ACC_CM_S_S = 69.8; // 1000 degrees/s/s
	protected final static int MAX_SPEED_CM_S = 50; // 50 cm per second

	private boolean my_goal_left = true;
	private WorldStateObserver mObs;
	private Thread mVisionThread;
	private MessageQueue mQueue = null;
	protected Communicator mComm = null;
	


	Point2D.Double enemy_goal = new Point2D.Double(my_goal_left ? PITCH_WIDTH_CM : 0, GOAL_Y_CM);


	protected mode state = mode.sit;

	// for low pass filtering
	protected WorldState worldState = null;
	// this is the amount of filtering to be done
	// higher values mean that the new data will "weigh more"
	// so the more uncertainty in result, the smaller value you should use
	// don't use values less then 1!
	private int filteredPositionAmount = 6;
	private int filteredAngleAmount = 2;

	protected Robot robot;

	/**
	 * Initialise the AI
	 * 
	 * @param Comm a communicator for making connection with real robot/simulated one
	 * @param Obs an observer for taking information about the table
	 */
	public AI(Communicator Comm, WorldStateProvider Obs) {
		this.mObs = new WorldStateObserver(Obs);
		mQueue = new MessageQueue(Comm);
		this.mComm = Comm;
	}

	/**
	 * Change mode. Can be used for penalty, freeplay, testing, etc
	 */
	public void setMode(mode new_mode) {
		state = new_mode;
	}

	/**
	 * Starts the AI in a new decision thread. (Not true, starts a new thread that updates the world state every time it changes)
	 * 
	 * Don't start more than once!
	 * @param my_team_blue true if my team is blue, false if my team is yellow
	 * @param my_goal_left true if my goal is on the left of camera, false otherwise
	 */
	public void start(final boolean my_team_blue, final boolean my_goal_left) {
		this.my_goal_left = my_goal_left;
		enemy_goal = new Point2D.Double(my_goal_left ? PITCH_WIDTH_CM : 0, GOAL_Y_CM);
		mVisionThread = new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					WorldState state = toCentimeters(mObs.getNextState());
					// do low pass filtering
					if (worldState == null)
						worldState = state;
					else
						worldState = new WorldState(
								lowPass(worldState.getBallCoords(), state.getBallCoords()),
								lowPass(worldState.getBlueRobot(), state.getBlueRobot()),
								lowPass(worldState.getYellowRobot(), state.getYellowRobot()),
								state.getWorldImage());
					robot = my_team_blue ? worldState.getBlueRobot() : worldState.getYellowRobot();
					// pass coordinates to decision making logic

					setChanged();
					notifyObservers(worldState);
					worldChanged();

				}
			}
		};
		mVisionThread.start();
	}



	/**
	 * Stops the AI
	 */
	public void stop() {
		if (mVisionThread != null)
			mVisionThread.interrupt();
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

	// Helpers

	private Point2D.Double toCentimeters(Point2D.Double original) {
		return new Point2D.Double(original.getX()*PITCH_WIDTH_CM, original.getY()*PITCH_WIDTH_CM);
	}
	
	private Robot toCentimeters(Robot orig) {
		return new Robot(toCentimeters(orig.getCoords()), orig.getAngle());
	}
	
	private WorldState toCentimeters(WorldState orig) {
		return new WorldState(
				toCentimeters(orig.getBallCoords()),
				toCentimeters(orig.getBlueRobot()),
				toCentimeters(orig.getYellowRobot()),
				orig.getWorldImage());
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
	 * A simple low-pass filter
	 * @param old_value
	 * @param new_value
	 * @param amount
	 * @return a filtered value
	 */
	private double lowPass(double old_value, double new_value, int amount) {
		return (old_value+new_value*amount)/((double) (amount+1));
	}

	/**
	 * Low pass for angles
	 * @param old_value
	 * @param new_value
	 * @return the filtered angle
	 */
	private double lowPass(double old_value, double new_value) {
		return lowPass(old_value, new_value, filteredAngleAmount);
	}

	/**
	 * Low pass on position
	 * @param old_value
	 * @param new_value
	 * @param amount
	 * @return the filtered position
	 */
	private Point2D.Double lowPass(Point2D.Double old_value, Point2D.Double new_value) {
		return new Point2D.Double (
				lowPass(old_value.getX(), new_value.getX(), filteredPositionAmount),
				lowPass(old_value.getY(), new_value.getY(), filteredPositionAmount));
	}

	/**
	 * Low pass on a robot
	 * @param old_value
	 * @param new_value
	 * @param amount
	 * @return a new robot with low_pass
	 */
	private Robot lowPass(Robot old_value, Robot new_value) {
		return new Robot(
				lowPass(old_value.getCoords(), new_value.getCoords()),
				lowPass(old_value.getAngle(), new_value.getAngle()));
	}


	protected abstract void worldChanged();


}
