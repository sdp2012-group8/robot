package sdp.AI;

import java.awt.geom.Point2D;
import java.io.IOException;

import sdp.common.Communicator;
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
public class AI {

	public enum mode {
		chase_once, chase_ball, sit
	}

	// pitch constants
	private final static double PITCH_WIDTH_CM = 244;
	private final static double goal_y_cm = 113.7/2;
	// robot constants
	private final static double TURNING_ACCURACY = 0;
	private final static double ROBOT_RADIUS_CM = 7;

	private final static double ROBOT_ACC_CM_S_S = 69.8; // 1000 degrees/s/s
	private final static int MAX_SPEED_CM_S = 50; // 50 cm per second

	private boolean my_team_blue = true;
	private boolean my_goal_left = true;
	private WorldStateObserver mObs;
	private Thread mVisionThread;
	private MessageQueue mQueue = null;
	private Communicator mComm = null;

	private mode state = mode.sit;

	// for low pass filtering
	private WorldState worldState = null;
	// this is the amount of filtering to be done
	// higher values mean that the new data will "weigh more"
	// so the more uncertainty in result, the smaller value you should use
	// don't use values less then 1!
	private int filteredPositionAmount = 6;
	private int filteredAngleAmount = 2;

	private Robot robot;

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
		switch (new_mode) {
		case chase_once:
			firstrun = true;
			break;
		case chase_ball:
			this.state = mode.chase_ball;
			break;
		}
	}

	/**
	 * Starts the AI in a new decision thread. (Not true, starts a new thread that updates the world state every time it changes)
	 * 
	 * Don't start more than once!
	 * @param my_team_blue true if my team is blue, false if my team is yellow
	 * @param my_goal_left true if my goal is on the left of camera, false otherwise
	 */
	public void start(final boolean my_team_blue, final boolean my_goal_left) {
		this.my_team_blue = my_team_blue;
		this.my_goal_left = my_goal_left;
		mVisionThread = new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					WorldState state = mObs.getNextState();
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
					worldChanged();
				}
			}
		};
		mVisionThread.start();
	}

	/**
	 * @return the most recent world state
	 */
	public WorldState getLatestWorldState() {
		return worldState;
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

	/**
	 * Gets the angle between two points
	 * @param A
	 * @param B
	 * @return if you stand at A how many degrees should you turn to face B
	 */
	private double anglebetween(Point2D.Double A, Point2D.Double B) {
		return (180*Math.atan2(B.getY()-A.getY(), B.getX()-A.getX()))/Math.PI;
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

	// Decision making:
	// Table coordinates:
	//        __________ y = 1.137 m
	// Entr  |          |
	//       |__________|
	// Right x=2.44 m   x=0, y = 0, Left goal
	//                       room 3.04

	private boolean firstrun = false;

	/**
	 * This method is fired when a new state is available. Decisions should be done here.
	 * @param new_state the new world state (low-pass filtered)
	 */
	private synchronized void worldChanged() {

		switch (state) {
		case chase_ball:
			chaseBall();
			break;
		}


		//		Point2D.Double ball = toCentimeters(new_state.getBallCoords());
		//		Robot my_robot = my_team_blue ? new_state.getBlueRobot() : new_state.getYellowRobot();
		//		@SuppressWarnings("unused")
		//		Point2D.Double my_goal = new Point2D.Double(my_goal_left ? 0 : PITCH_WIDTH_CM, goal_y_cm);
		//		Robot enemy_robot = my_team_blue ? new_state.getYellowRobot() : new_state.getBlueRobot();
		//		Point2D.Double enemy_goal = new Point2D.Double(my_goal_left ? PITCH_WIDTH_CM : 0, goal_y_cm);
		//		// start logic
		//		if (firstrun) {
		//			firstrun = false;
		//			goTo(my_robot, ball, anglebetween(ball, enemy_goal));
		//			System.out.println("Ball at (" + ball.x +", " + ball.y + "), " +"My at (" + my_robot.getCoords().x +", " + my_robot.getCoords().y +", " + my_robot.getAngle() + "), " +"Enemy at (" + enemy_robot.getCoords().x +", " + enemy_robot.getCoords().y +", " + enemy_robot.getAngle() + ").");
		//		}
		//System.out.println("Ball at (" + ball.x +", " + ball.y + "), " +"My at (" + my_robot.getCoords().x +", " + my_robot.getCoords().y +", " + my_robot.getAngle() + "), " +"Enemy at (" + enemy_robot.getCoords().x +", " + enemy_robot.getCoords().y +", " + enemy_robot.getAngle() + ").");
	}

	public void chaseBall() {
		// System.out.println("Chasing ball");
		double angle_between = anglebetween(robot.getCoords(), worldState.getBallCoords());
		int distance = (int) Tools.getDistanceBetweenPoint(robot.getCoords(), worldState.getBallCoords());
		int turning_angle = (int) (- (180*robot.getAngle())/Math.PI - angle_between);
		
		// Keep the turning angle between -180 and 180
		if (turning_angle > 180) turning_angle -= 360;
		if (turning_angle < -180) turning_angle += 360;
		try {
			if (turning_angle > TURNING_ACCURACY){
				if (turning_angle > 127) turning_angle = 127;
				if (turning_angle < -128) turning_angle = -128;// Needs to reduce the angle as the command can only accept -128 to 127
				mComm.sendMessage(opcode.operate, (byte)0, (byte)127);
				//mComm.sendMessage(opcode.turn, (byte)turning_angle);
				System.out.println("Chasing ball - Turning: " + turning_angle);
			} 
			else if( turning_angle < -TURNING_ACCURACY){
				mComm.sendMessage(opcode.operate, (byte)0, (byte)-127);
				//mComm.sendMessage(opcode.turn, (byte)turning_angle);
				System.out.println("Chasing ball - Turning: " + turning_angle);	
			}
			else if (distance != 0) {

				// mComm.sendMessage(opcode.operate, (byte)1, (byte)0);
				System.out.println("Chasing ball - Moving Forward");
			} else {
				System.out.println("Chasing ball - At Ball");
			}
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * Calculates and performs the movements required to go from current position
	 * to the position final_position and facing the angle final_angle.
	 * 
	 * @param my_robot	Object representing our robot.
	 * @param final_position	Position the robot has to move to.
	 * @param final_angle	Angle the robot should end up facing.
	 */
	private void goTo(Robot my_robot, Point2D.Double final_position, double final_angle) {
		Point2D.Double my_robot_coords = toCentimeters(my_robot.getCoords());
		double distance = Tools.getDistanceBetweenPoint(my_robot_coords, final_position);

		if (distance < 20) {
			System.out.println("Goal reached!");
			return;
		}
		// distance to front of robot
		double angle_between = anglebetween(my_robot_coords, final_position);
		double turning_angle1= - my_robot.getAngle() - angle_between*180/Math.PI;
		double turning_angle2= - angle_between*180/Math.PI - final_angle*180/Math.PI;
		if (turning_angle1 > 180)
			turning_angle1 -= 360;
		else if (turning_angle1 < -180)
			turning_angle1 += 360;
		if (turning_angle2 > 180)
			turning_angle2 -= 360;
		else if (turning_angle2 < -180)
			turning_angle2 += 360;
		// time required for acceleration to max_speed
		double acc_t = MAX_SPEED_CM_S/ROBOT_ACC_CM_S_S;
		// distance required for acceleration to max speed
		double acc_distance = ROBOT_ACC_CM_S_S*acc_t*acc_t/2d;
		// time required travelling with constant speed
		double const_spd_time = (distance - acc_distance*2)/MAX_SPEED_CM_S;
		// calculate total time in the two cases:
		// 1. where the robot won't have enough time to accelerate
		// 2. otherwise
		double time = const_spd_time < 0 ? Math.sqrt(distance/ROBOT_ACC_CM_S_S) : acc_t+const_spd_time;
		double turning_speed1 = 2 * turning_angle1 / time;
		double turning_speed2 = 2 * turning_angle2 / time;
		if (turning_speed1 > 128 || turning_speed2 > 128)
			System.out.println("!!!!!!COMMAND OVERFLOW!!!!!!!!!");
		mQueue.addMessageToQueue(0, opcode.operate, (byte) MAX_SPEED_CM_S, (byte) turning_speed1);
		mQueue.addMessageToQueue(time/2, opcode.operate, (byte) MAX_SPEED_CM_S, (byte) turning_speed2);
		mQueue.addMessageToQueue(time, opcode.operate, (byte) 0, (byte) 0);
		System.out.println("Expected runtime "+time+"s; distance is "+(int) distance+", tirning_angle2 "+(int) turning_angle2+"; my angle "+(int) my_robot.getAngle()+"; final angle "+(int) (final_angle*180/Math.PI)+"; face_angle "+(int) (angle_between*180/Math.PI));
	}

}
