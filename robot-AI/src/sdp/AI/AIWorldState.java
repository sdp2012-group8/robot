package sdp.AI;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import sdp.common.Goal;
import sdp.common.Robot;
import sdp.common.Tools;
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
			robot = getYellowRobot();
			enemy_robot = getBlueRobot();
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
		} else if (Tools.pointInTriangle(getBallCoords(), top, bottom, intersection)) {
			return 0;
		}
		
		//if it gets here it can see the goal
		if (Tools.isPathClear(getBallCoords(), enemy_goal.getCentre(), enemy_robot)) {
			return 2;
		} else if (Tools.isPathClear(getBallCoords(), enemy_goal.getTop(), enemy_robot)) {
			return 1;
		} else if (Tools.isPathClear(getBallCoords(), enemy_goal.getBottom(), enemy_robot)) {
			return 3;
		}
		
		return -1; //should never be reached
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
