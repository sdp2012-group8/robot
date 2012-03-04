package sdp.AI;

import java.awt.geom.Point2D;
import sdp.common.Tools;
import sdp.common.Utilities;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;
import sdp.common.Robot;
import sdp.common.WorldStateProvider;

/**
 * 
 * This is the AIListener class that updates the world the AI sees.
 * 
 * @author Martin Marinov
 *
 */

public abstract class AIListener extends WorldStateProvider {
	
	private WorldStateObserver mObs;
	private Thread mVisionThread;

	// for low pass filtering
	protected WorldState world_state = null;
	
	protected AIWorldState ai_world_state;
	private boolean my_goal_left, my_team_blue;
	
	// this is the amount of filtering to be done
	// higher values mean that the new data will "weigh more"
	// so the more uncertainty in result, the smaller value you should use
	// don't use values less then 1!
	private int filteredPositionAmount = 6;
	private int filteredAngleAmount = 2;

	/**
	 * Initialise the AI
	 * 
	 * @param Comm a communicator for making connection with real robot/simulated one
	 * @param Obs an observer for taking information about the table
	 */
	public AIListener(WorldStateProvider Obs) {
		this.mObs = new WorldStateObserver(Obs);
	}

	/**
	 * Starts the AI in a new decision thread. (Not true, starts a new thread that updates the world state every time it changes)
	 * 
	 * Don't start more than once!
	 * @param my_team_blue true if my team is blue, false if my team is yellow
	 * @param my_goal_left true if my goal is on the left of camera, false otherwise
	 */
	public void start(final boolean is_my_team_blue, final boolean is_my_goal_left) {
		this.my_team_blue = is_my_team_blue;
		this.my_goal_left = is_my_goal_left;
		mVisionThread = new Thread() {
			@Override
			public void run() {
				while (!isInterrupted()) {
					WorldState state = Utilities.toCentimeters(mObs.getNextState());
					// do low pass filtering
					if (world_state == null) {
						world_state = state;
						ai_world_state = new AIWorldState(world_state, my_team_blue, my_goal_left);
					} else {
						world_state = new WorldState(checkBall(state.getBallCoords(), world_state.getBallCoords()),
								lowPass(world_state.getBlueRobot(), state.getBlueRobot()),
								lowPass(world_state.getYellowRobot(), state.getYellowRobot()),
								state.getWorldImage());
					}
					
					ai_world_state.update(world_state, my_team_blue, my_goal_left);
					
					// pass coordinates to decision making logic
					setChanged();
					notifyObservers(world_state);
					worldChanged();
					
				}
			}
		};
		mVisionThread.start();
	}
	
	public void updateGoalOrTeam(final boolean is_my_team_blue, final boolean is_my_goal_left) {
		this.my_team_blue = is_my_team_blue;
		this.my_goal_left = is_my_goal_left;
	}


	/**
	 * Stops the AI
	 */
	public void stop() {
		if (mVisionThread != null)
			mVisionThread.interrupt();
	}
	
	/**
	 * Fix ball going offscreen
	 * @param new_coords
	 * @return
	 */
	private Point2D.Double checkBall(Point2D.Double new_coords, Point2D.Double old_coords) {
		if (new_coords.getX() == -244d && new_coords.getY() == -244d)
			return old_coords;
		else
			return new_coords;
	}

	/**
	 * A simple low-pass filter
	 * @param old_value
	 * @param new_value
	 * @param amount
	 * @return a filtered value
	 */
	private double lowPass(double old_value, double new_value, int amount) {
		if (Double.isNaN(new_value) || Double.isInfinite(new_value))
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
		Robot a = new Robot(
				lowPass(old_value.getCoords(), new_value.getCoords()),
				lowPass(old_value.getAngle(), new_value.getAngle()));
		a.setCoords(true);
		return a;
	}
	
	protected abstract void worldChanged();

}
