package sdp.AI;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import sdp.common.Communicator;
import sdp.common.Goal;
import sdp.common.MessageQueue;
import sdp.common.Tools;
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
		chase_ball, sit, got_ball, dribble
	}


	// robot constants
	protected final static double TURNING_ACCURACY = 10;

	protected final static double ROBOT_ACC_CM_S_S = 69.8; // 1000 degrees/s/s
	protected final static int MAX_SPEED_CM_S = 50; // 50 cm per second
	protected final static int MAX_TURNING_SPEED = 70;

	protected boolean my_goal_left = true;
	private WorldStateObserver mObs;
	private Thread mVisionThread;
	private MessageQueue mQueue = null;
	protected Communicator mComm = null;
	
	private WorldState old_ws = null;

	Goal enemy_goal = new Goal(new Point2D.Double(my_goal_left ? Tools.PITCH_WIDTH_CM : 0, Tools.GOAL_Y_CM));

	protected mode state = mode.sit;

	// for low pass filtering
	protected WorldState worldState = null;
	// this is the amount of filtering to be done
	// higher values mean that the new data will "weigh more"
	// so the more uncertainty in result, the smaller value you should use
	// don't use values less then 1!
	private int filteredPositionAmount = 6;
	private int filteredAngleAmount = 2;
	
	//flags
	private boolean f_ball_on_field = true;

	protected Robot robot;
	protected Robot enemy_robot;

	protected boolean am_i_blue;

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
	 * Gets AI mode
	 * @return
	 */
	public mode getMode() {
		return state;
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
		this.am_i_blue = my_team_blue;
		enemy_goal = new Goal(new Point2D.Double(my_goal_left ? Tools.PITCH_WIDTH_CM : 0, Tools.GOAL_Y_CM));
		mVisionThread = new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					WorldState state = Tools.toCentimeters(mObs.getNextState());
					// do low pass filtering
					if (worldState != null)
						old_ws = new WorldState(worldState.getBallCoords(), worldState.getBlueRobot(), worldState.getYellowRobot(), worldState.getWorldImage());
					if (worldState == null)
						worldState = state;
					else
						worldState = new WorldState(
								lowPass(checkBall(state.getBallCoords()), worldState.getBallCoords()),
								lowPass(worldState.getBlueRobot(), state.getBlueRobot()),
								lowPass(worldState.getYellowRobot(), state.getYellowRobot()),
								state.getWorldImage());

					if (my_team_blue) {
						robot = worldState.getBlueRobot();
						enemy_robot = worldState.getYellowRobot();
					} else {
						robot = worldState.getYellowRobot();
						enemy_robot = worldState.getBlueRobot();
					}
					
					// check and set flags
					if (worldState.getBallCoords() == new Point2D.Double(-1,-1)) {
						f_ball_on_field = false;
					}
					
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
	 * Fix ball going offscreen
	 * @param new_coords
	 * @return
	 */
	private Point2D.Double checkBall(Point2D.Double new_coords) {
		if (new_coords.getX() == -244d && new_coords.getY() == -244d)
			return old_ws.getBallCoords();
		else
			return new_coords;
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
		Robot a = new Robot(
				lowPass(old_value.getCoords(), new_value.getCoords()),
				lowPass(old_value.getAngle(), new_value.getAngle()));
		a.setCoords(true);
		return a;
	}

	
	/**
	 * Comparator to sort Point2D.Double's into descending order of y values
	 * @author michael
	 *
	 */
	private class yPoints implements Comparator<Point2D.Double> {

		@Override
		public int compare(Point2D.Double o1, Point2D.Double o2) {
			return (o1.y > o2.y ? -1 : (o1.y == o2.y ? 0 : 1));
		}
		
	}
	
	/**
	 * Comparator to sort Point2D.Double's into descending order of x values
	 * @author michael
	 *
	 */
	private class xPoints implements Comparator<Point2D.Double> {

		@Override
		public int compare(Point2D.Double o1, Point2D.Double o2) {
			return (o1.x > o2.x ? -1 : (o1.x == o2.x ? 0 : 1));
		}
		
	}
	
	/**
	 * Calculates if the ball has a direct line of sight to the enemy goal.
	 * @param enemy_robot The robot defending the goal
	 * @param enemy_goal The goal of the opposing robot
	 * @return -1 if error, 0 if false, 1 if can see top, 2 middle, 3 bottom.
	 */
	public int isGoalVisible(Robot enemy_robot, Goal enemy_goal) {
		enemy_robot.setCoords(true); //need to convert robot coords to cm
		
		//System.out.println("goal.top: " + enemy_goal.getTop() + "  goal.bottom: " + enemy_goal.getBottom() + "  robot.left: " + enemy_robot.getFrontLeft() + "  robot.right: " + enemy_robot.getFrontRight());
		//System.out.println("Ball: " + worldState.getBallCoords() + "  frontleft: " + enemy_robot.getFrontLeft() + "  frontRight: " + enemy_robot.getFrontRight() + "  inter: " + intersection); 
		// TODO: change robot points from front to more accurate ones (flipper's / max/min y values)
		
		// Array of the 3 possible points on the robot
		Point2D.Double[] points = new Point2D.Double[3];
		// List of all the points on the robot so we can sort them
		List<Point2D.Double> e_robot_points = new ArrayList<Point2D.Double>(); 
		e_robot_points.add(enemy_robot.getBackLeft());
		e_robot_points.add(enemy_robot.getFrontLeft());
		e_robot_points.add(enemy_robot.getBackRight());
		e_robot_points.add(enemy_robot.getFrontRight());
		
		// Sorting to find the point with the highest and lowest y value
		Collections.sort(e_robot_points, new yPoints());
		points[0] = e_robot_points.get(0);
		points[1] = e_robot_points.get(3);
		
		// Sorting to find the 2 points with the highest x value
		Collections.sort(e_robot_points, new xPoints());
		
		// The value in the top two that isn't already in points[] is the one we want.
		if (e_robot_points.get(0) == points[0] || e_robot_points.get(0) == points[1]) {
			points[2] = e_robot_points.get(0);
		} else {
			points[2] = e_robot_points.get(1);
		}
		
		Point2D.Double top;
		Point2D.Double bottom;
		
		// Find top point by checking if two of the points are on the same side of the
		// line between the third point and the top of the goal
		if (Tools.sameSide(points[1], points[2], enemy_goal.getTop(), points[0])) {
			top = points[0];
		} else {
			top = points[2];
		}
		
		// Find bottom Point
		if (Tools.sameSide(points[0], points[2], enemy_goal.getTop(), points[1])) {
			bottom = points[1];
		} else {
			bottom = points[2];
		}
		
		// Finds the intersection of the lines from the top and bottom of the goals to the robot corners.
		Point2D.Double intersection = Tools.intersection(enemy_goal.getTop(), top, enemy_goal.getBottom(), bottom);
		if ((intersection == null)) {
			return -1;
		} else if (Tools.pointInTriangle(worldState.getBallCoords(), top, bottom, intersection)) {
			return 0;
		}
		
		//if it gets here it can see the goal
		if (Tools.isPathClear(worldState.getBallCoords(), enemy_goal.getCentre(), enemy_robot)) {
			return 2;
		} else if (Tools.isPathClear(worldState.getBallCoords(), enemy_goal.getTop(), enemy_robot)) {
			return 1;
		} else if (Tools.isPathClear(worldState.getBallCoords(), enemy_goal.getBottom(), enemy_robot)) {
			return 3;
		}
		
		return -1; //should never be reached
	}
	
	//public int canWeShoot() {
		
	//}
	
	protected abstract void worldChanged();

}
