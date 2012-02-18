package sdp.AI;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import sdp.common.Goal;
import sdp.common.Robot;
import sdp.common.Tools;
import sdp.common.Utilities;
import sdp.common.Vector2D;
import sdp.common.WorldState;

public class AIWorldState extends WorldState {
	
	public enum mode {
		chase_ball, sit, got_ball, dribble
	}
	
	private final boolean my_team_blue;
	private final boolean my_goal_left;
	private final Goal enemy_goal;
	private final Goal my_goal;
	
	//changing variables
	private Robot robot = null;
	private Robot enemy_robot = null;
	private mode state = mode.sit;
	private double distance_to_ball;
	private double distance_to_goal;
	
	//flags
	boolean f_ball_on_field = false;
	
	
	public AIWorldState(WorldState world_state, boolean my_team_blue, boolean my_goal_left) {
		super(world_state.getBallCoords(), world_state.getBlueRobot(),world_state.getYellowRobot(), world_state.getWorldImage());
		this.my_team_blue = my_team_blue;
		this.my_goal_left = my_goal_left;

		if (my_goal_left) {
			enemy_goal = new Goal(new Point2D.Double(Tools.PITCH_WIDTH_CM , Tools.GOAL_Y_CM ));
			my_goal = new Goal(new Point2D.Double(0 , Tools.GOAL_Y_CM ));
		} else {
			enemy_goal = new Goal(new Point2D.Double(0 , Tools.GOAL_Y_CM ));
			my_goal = new Goal(new Point2D.Double(Tools.PITCH_WIDTH_CM , Tools.GOAL_Y_CM ));
		}
		
		update(world_state);
	}
	
	public void update(WorldState world_state) {
				
		super.update(world_state.getBallCoords(), world_state.getBlueRobot(),world_state.getYellowRobot(), world_state.getWorldImage());
		//update variables
		if (my_team_blue) {
			robot = getBlueRobot();
			enemy_robot = getYellowRobot();
		} else {
			robot = (getYellowRobot());
			enemy_robot = (getBlueRobot());
		}
		
		
		distance_to_ball = Tools.getDistanceBetweenPoint(Tools.getGlobalVector(robot, new Vector2D(Robot.LENGTH_CM/2, 0)), getBallCoords());
		distance_to_goal = Tools.getDistanceBetweenPoint(robot.getCoords(), enemy_goal.getCentre());
		
		// check and set flags
		if (getBallCoords() == new Point2D.Double(-1,-1)) {
			f_ball_on_field = false;
		}
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
	 * Calculates if the ball has a direct line of sight to the enemy goal.
	 * @param enemy_robot The robot defending the goal
	 * @param enemy_goal The goal of the opposing robot
	 * @return -1 if error, 0 if false, 1 if can see top, 2 middle, 3 bottom.
	 */
	public int isGoalVisible(Robot enemy_robot, Goal enemy_goal) {
		
		enemy_robot.setCoords(true); //need to convert robot coords to cm
		
		if (Utilities.isPathClear(getBallCoords(), enemy_goal.getCentre(), enemy_robot)) {
			return 2;
		} else if (Utilities.isPathClear(getBallCoords(), enemy_goal.getTop(), enemy_robot)) {
			return 1;
		} else if (Utilities.isPathClear(getBallCoords(), enemy_goal.getBottom(), enemy_robot)) {
			return 3;
		}
		
		//Check for "special" case where the ball is between the enemy robot and their goal.
		if (enemy_goal.getCentre().x == 0) {
			if (getBallCoords().x < enemy_robot.getCoords().x) return 2;
		} else {
			if (getBallCoords().x > enemy_robot.getCoords().x) return 2;
		}
		
		return 0; //can't see goal
	}

	public Robot getEnemyRobot() {
		return enemy_robot;
	}

	public Robot getRobot() {
		return robot;
	}
	
	public double getDistanceToBall() {
		return distance_to_ball;
	}
	
	public double getDistanceToGoal() {
		return distance_to_goal;
	}

	public Goal getEnemyGoal() {
		return enemy_goal;
	}
	
	public Goal getMyGoal() {
		return my_goal;
	}

	public boolean getMyGoalLeft() {
		return my_goal_left;
	}
	
	public boolean getMyTeamBlue() {
		return my_team_blue;
	}
}
