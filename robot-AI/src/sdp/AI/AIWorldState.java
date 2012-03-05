package sdp.AI;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import sdp.common.Goal;
import sdp.common.Painter;
import sdp.common.Robot;
import sdp.common.Utilities;
import sdp.common.Vector2D;
import sdp.common.WorldState;
import sdp.vision.processing.ImageProcessorConfig;

public class AIWorldState extends WorldState {

	private boolean my_team_blue;
	private boolean my_goal_left;
	private Goal enemy_goal;
	private Goal my_goal;

	//changing variables
	private Robot robot = null;
	private Robot enemy_robot = null;
	private double distance_to_ball;
	private double distance_to_goal;

	//flags
	boolean f_ball_on_field = false;
	

	public AIWorldState(WorldState world_state, boolean my_team_blue, boolean my_goal_left) {
		super(world_state.getBallCoords(), world_state.getBlueRobot(),world_state.getYellowRobot(), world_state.getWorldImage());


		update(world_state, my_team_blue, my_goal_left);
	}

	public void update(WorldState world_state, boolean my_team_blue, boolean my_goal_left) {

		this.my_team_blue = my_team_blue;
		this.my_goal_left = my_goal_left;

		if (my_goal_left) {
			enemy_goal = new Goal(new Point2D.Double(WorldState.PITCH_WIDTH_CM , WorldState.GOAL_Y_CM ));
			my_goal = new Goal(new Point2D.Double(0 , WorldState.GOAL_Y_CM ));
		} else {
			enemy_goal = new Goal(new Point2D.Double(0 , WorldState.GOAL_Y_CM ));
			my_goal = new Goal(new Point2D.Double(WorldState.PITCH_WIDTH_CM , WorldState.GOAL_Y_CM ));
		}

		super.update(world_state.getBallCoords(), world_state.getBlueRobot(),world_state.getYellowRobot(), world_state.getWorldImage());
		//update variables
		if (my_team_blue) {
			robot = getBlueRobot();
			enemy_robot = getYellowRobot();
		} else {
			robot = (getYellowRobot());
			enemy_robot = (getBlueRobot());
		}


		distance_to_ball = Utilities.getDistanceBetweenPoint(Utilities.getGlobalVector(robot, new Vector2D(Robot.LENGTH_CM/2, 0)), getBallCoords());
		distance_to_goal = Utilities.getDistanceBetweenPoint(robot.getCoords(), enemy_goal.getCentre());

		// check and set flags
		if (getBallCoords() == new Point2D.Double(-1,-1)) {
			f_ball_on_field = false;
		}
	}



	////////////////////////////////////////////////////////////////
	// Methods
	///////////////////////////////////////////////////////////////


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
	
	public void onDraw(BufferedImage im, ImageProcessorConfig config) {
		Painter p = new Painter(im, this);
		p.setOffsets(config.getFieldLowX(), config.getFieldLowY(), config.getFieldWidth(), config.getFieldHeight());
		p.image();
		p.dispose();
	}


	////////////////////////////////////////////////////////
	// getters and setters
	///////////////////////////////////////////////////////
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
