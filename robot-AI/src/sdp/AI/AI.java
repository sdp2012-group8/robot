package sdp.AI;

import java.awt.geom.Point2D;

import sdp.common.Communicator;
import sdp.common.MessageQueue;
import sdp.common.Tools;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;
import sdp.common.Robot;
import sdp.common.Communicator.opcode;

/**
 * 
 * This is the AI class that will take desicions.
 * 
 * @author Martin Marinov
 *
 */
public class AI {
	
	public enum mode {
		chase_once
	}
	
	// pitch constants
	private final static double pitch_width_cm = 244;
	private final static double door_y_cm = 113.7/2;
	// robot constants
	private final static double robot_acc_cm_s_s = 69.8; // 1000 rev/s/s
	private final static int max_speed_cm_s = 50; // 50 cm per second
	
	private boolean my_team_blue = true;
	private boolean my_door_left = true;
	private WorldStateObserver mObs;
	private Thread mVisionThread;
	private MessageQueue mQueue = null;
	
	// for low pass filtering
	private WorldState filteredState = null;
	// this is the amount of filtering to be done
	// higher values mean that the new data will "weigh more"
	// so the more uncertainty in result, the smaller value you should use
	// don't use values less then 1!
	private int filteredPositionAmount = 6;
	private int filteredAngleAmount = 2;
	
	/**
	 * Initialise the AI
	 * 
	 * @param Comm a communiactor for making connection with real robot/simulated one
	 * @param Obs an observer for taking information about the table
	 */
	public AI(Communicator Comm, WorldStateObserver Obs) {
		this.mObs = Obs;
		mQueue = new MessageQueue(Comm);
	}
	
	/**
	 * Change mode. Can be used for penalty, freeplay, testing, etc
	 */
	public void setMode(mode new_mode) {
		switch (new_mode) {
		case chase_once:
			firstrun = true;
			break;
		}
	}
	 
	/**
	 * Starts the AI in a new decision thread.
	 * 
	 * Don't start more than once!
	 * @param my_team_blue true if my team is blue, false if my team is yellow
	 * @param my_door_left true if my door is on the left of camera, false otherwise
	 */
	public void start(boolean my_team_blue, boolean my_door_left) {
		this.my_team_blue = my_team_blue;
		this.my_door_left = my_door_left;
		mVisionThread = new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					WorldState state = mObs.getNextState();
					// do low pass filtering
					if (filteredState == null)
						filteredState = state;
					else
						filteredState = new WorldState(
								lowPass(filteredState.getBallCoords(), state.getBallCoords()),
								lowPass(filteredState.getBlueRobot(), state.getBlueRobot()),
								lowPass(filteredState.getYellowRobot(), state.getYellowRobot()),
								state.getWorldImage());
					// pass coordinates to decision making logic
					worldChanged(filteredState);
				}
			}
		};
		mVisionThread.start();
	}
	
	/**
	 * @return the most recent world state
	 */
	public WorldState getLatestWorldState() {
		return filteredState;
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
		return new Point2D.Double(original.getX()*pitch_width_cm, original.getY()*pitch_width_cm);
	}
	
	/**
	 * Gets the angle between two points
	 * @param A
	 * @param B
	 * @return if you stand at A how many degrees (in rad) should you turn to face B
	 */
	private double anglebetween(Point2D.Double A, Point2D.Double B) {
		return Math.atan2(B.getY()-A.getY(), B.getX()-A.getX());
	}
	
	
	/**
	 * A simple low-pass filter
	 * @param old_value
	 * @param new_value
	 * @param amount
	 * @return a filtered value
	 */
	private double lowPass(double old_value, double new_value, int amount) {
		if (new_value < 0)
			return old_value;
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
	
	// Decision making:
	// Table coordinates:
	//        __________ y = 1.137 m
	// Entr  |          |
	//       |__________|
	// Right x=2.44 m   x=0, y = 0, Left door
	//                       room 3.04
	
	private boolean firstrun = false;
	
	/**
	 * This method is fired when a new state is available. Decisions should be done here.
	 * @param new_state the new world state (low-pass filtered)
	 */
	private void worldChanged(WorldState new_state) {
		Point2D.Double ball = toCentimeters(new_state.getBallCoords());
		Robot my_robot = my_team_blue ? new_state.getBlueRobot() : new_state.getYellowRobot();
		@SuppressWarnings("unused")
		Point2D.Double my_door = new Point2D.Double(my_door_left ? 0 : pitch_width_cm, door_y_cm);
		Robot enemy_robot = my_team_blue ? new_state.getYellowRobot() : new_state.getBlueRobot();
		Point2D.Double enemy_door = new Point2D.Double(my_door_left ? pitch_width_cm : 0, door_y_cm);
		// start logic
		if (firstrun) {
			firstrun = false;
			goTo(my_robot, ball, anglebetween(ball, enemy_door));
			System.out.println("Ball at (" + ball.x +", " + ball.y + "), " +"My at (" + my_robot.getCoords().x +", " + my_robot.getCoords().y +", " + my_robot.getAngle() + "), " +"Enemy at (" + enemy_robot.getCoords().x +", " + enemy_robot.getCoords().y +", " + enemy_robot.getAngle() + ").");
		}
		//System.out.println("Ball at (" + ball.x +", " + ball.y + "), " +"My at (" + my_robot.getCoords().x +", " + my_robot.getCoords().y +", " + my_robot.getAngle() + "), " +"Enemy at (" + enemy_robot.getCoords().x +", " + enemy_robot.getCoords().y +", " + enemy_robot.getAngle() + ").");
	}
	
	private void goTo(Robot my_robot, Point2D.Double final_position, double final_angle) {
		Point2D.Double my_robot_coords = toCentimeters(my_robot.getCoords());
		double distance = Tools.getDistanceBetweenPoint(my_robot_coords, final_position);
		double turning_angle = -(final_angle - my_robot.getAngle())*180/Math.PI;
		// we want to go there with speed maxspeed, how many seconds will it take
		// distance/max_recommended_speed time needed for travelling with constant speed
		// Math.sqrt(2*max_recommended_speed/robot_acc_cm_s_s) time needed for decelerating
		double time = distance/max_speed_cm_s;// - Math.sqrt(2*max_speed_cm_s/robot_acc_cm_s_s);
		double turning_speed = turning_angle / time;
		mQueue.addMessageToQueue(0, opcode.operate, (byte) max_speed_cm_s, (byte) 0);//turning_speed);
		mQueue.addMessageToQueue(time, opcode.operate, (byte) 0, (byte) 0);
		System.out.println("Expected runtime "+time+"s; distance is "+distance);
		//System.out.println("Expected runtime "+time+"s; turning speed "+turning_speed+"; turning angle "+turning_angle+"; final angle "+final_angle);
	}

}