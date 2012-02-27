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
	 * @return true if goal is visible
	 */
	public boolean isGoalVisible() {
		
		enemy_robot.setCoords(true); //need to convert robot coords to cm
		
		if (Utilities.isPathClear(getBallCoords(), enemy_goal.getCentre(), enemy_robot)
				|| Utilities.isPathClear(getBallCoords(), enemy_goal.getTop(), enemy_robot) 
				|| Utilities.isPathClear(getBallCoords(), enemy_goal.getBottom(), enemy_robot)) {
			return true;
		}
		
		//Check for "special" case where the ball is between the enemy robot and their goal.
		if (enemy_goal.getCentre().x == 0) {
			if (getBallCoords().x < enemy_robot.getCoords().x) return true;
		} else {
			if (getBallCoords().x > enemy_robot.getCoords().x) return true;
		}
		
		return false; //can't see goal
	}
	
	public double calculateShootAngle() {
		double topmin = anglebetween(getRobot().getCoords(), enemy_goal.getTop());
		double midmin = anglebetween(getRobot().getCoords(), enemy_goal.getCentre());
		double botmin = anglebetween(getRobot().getCoords(), enemy_goal.getBottom());
		
		if (Math.abs(topmin) < Math.abs(midmin) && Math.abs(topmin) < Math.abs(botmin) && Utilities.isPathClear(getBallCoords(), enemy_goal.getTop(), enemy_robot)) {
			return topmin;
		} else if (Math.abs(midmin) < Math.abs(botmin) && Utilities.isPathClear(getBallCoords(), enemy_goal.getCentre(), enemy_robot)) {
			return midmin;
		} else {
			return botmin;
		}
		
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
