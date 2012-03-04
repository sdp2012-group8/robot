package sdp.AI;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Iterator;

import sdp.common.Goal;
import sdp.common.Robot;
import sdp.common.Tools;
import sdp.common.Utilities;
import sdp.common.Vector2D;
import sdp.common.WorldState;

public class AIWorldState extends WorldState {

	private static final double POINT_OFFSET = 2*Robot.LENGTH_CM;

	private boolean my_team_blue;
	private boolean my_goal_left;
	private Goal enemy_goal;
	private Goal my_goal;
	private final static double GOAL_SIZE = 60; // cm

	//changing variables
	private Robot robot = null;
	private Robot enemy_robot = null;
	private double distance_to_ball;
	private double distance_to_goal;

	//flags
	boolean f_ball_on_field = false;

	//imaginary top pitch
	private Point2D.Double[] imaginaryTopPitch = {new Point2D.Double(0,0),
			new Point2D.Double(Utilities.PITCH_WIDTH_CM,0),new Point2D.Double(Utilities.PITCH_WIDTH_CM,-Utilities.PITCH_HEIGHT_CM),
			new Point2D.Double(0,-Utilities.PITCH_HEIGHT_CM)};
	//imaginary bottom pitch
	private Point2D.Double[] imaginaryBottomPitch = {new Point2D.Double(0,Utilities.PITCH_HEIGHT_CM),
			new Point2D.Double(Utilities.PITCH_WIDTH_CM,Utilities.PITCH_HEIGHT_CM),new Point2D.Double(Utilities.PITCH_WIDTH_CM,2*Utilities.PITCH_HEIGHT_CM),
			new Point2D.Double(0,2*Utilities.PITCH_HEIGHT_CM)};
	//imaginary top enemy goal
	private Point2D.Double[] imaginaryTopEnemyGoal = {new Point2D.Double(0,0), new Point2D.Double(0,0)};
	private Point2D.Double[] imaginaryBottomEnemyGoal = {new Point2D.Double(0,0), new Point2D.Double(0,0)};


	public AIWorldState(WorldState world_state, boolean my_team_blue, boolean my_goal_left) {
		super(world_state.getBallCoords(), world_state.getBlueRobot(),world_state.getYellowRobot(), world_state.getWorldImage());


		update(world_state, my_team_blue, my_goal_left);
	}

	public void update(WorldState world_state, boolean my_team_blue, boolean my_goal_left) {

		this.my_team_blue = my_team_blue;
		this.my_goal_left = my_goal_left;

		if (my_goal_left) {
			enemy_goal = new Goal(new Point2D.Double(Utilities.PITCH_WIDTH_CM , Utilities.GOAL_Y_CM ));
			my_goal = new Goal(new Point2D.Double(0 , Utilities.GOAL_Y_CM ));
		} else {
			enemy_goal = new Goal(new Point2D.Double(0 , Utilities.GOAL_Y_CM ));
			my_goal = new Goal(new Point2D.Double(Utilities.PITCH_WIDTH_CM , Utilities.GOAL_Y_CM ));
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
	 * This checks if the robot can see the imaginary goals
	 * If it returns true, the robot can kick the ball into the wall and it should reflect towards the enemy goal
	 * This can be done more elegantly by using a rotation matrix, will look into that later(Laura)
	 * @return if you can shoot into the imaginary goal
	 */
	protected boolean goalImage(){
		if (my_goal_left){
			imaginaryTopEnemyGoal[0] = new Point2D.Double(Utilities.PITCH_WIDTH_CM,-Utilities.PITCH_HEIGHT_CM/2-GOAL_SIZE/2); //top point
			imaginaryTopEnemyGoal[1] = new Point2D.Double(Utilities.PITCH_WIDTH_CM,-Utilities.PITCH_HEIGHT_CM/2+GOAL_SIZE/2); //bottom point
			imaginaryBottomEnemyGoal[0] = new Point2D.Double(Utilities.PITCH_WIDTH_CM,3*Utilities.PITCH_HEIGHT_CM/2-GOAL_SIZE/2); //top point
			imaginaryBottomEnemyGoal[1] = new Point2D.Double(Utilities.PITCH_WIDTH_CM,3*Utilities.PITCH_HEIGHT_CM/2+GOAL_SIZE/2); //bottom point

		}
		else {
			imaginaryTopEnemyGoal[0] = new Point2D.Double(0,-Utilities.PITCH_HEIGHT_CM/2-GOAL_SIZE/2); //top point
			imaginaryTopEnemyGoal[1] = new Point2D.Double(0,-Utilities.PITCH_HEIGHT_CM/2+GOAL_SIZE/2); //bottom point
			imaginaryBottomEnemyGoal[0] = new Point2D.Double(0,3*Utilities.PITCH_HEIGHT_CM/2-GOAL_SIZE/2); //top point
			imaginaryBottomEnemyGoal[1] = new Point2D.Double(0,3*Utilities.PITCH_HEIGHT_CM/2+GOAL_SIZE/2); //bottom point
		}

		//robot in upper half of pitch, play ball with top wall
		if (getRobot().getCoords().y < Utilities.PITCH_HEIGHT_CM/2){ 
			Point2D.Double intersection = Utilities.intersection(getBallCoords(), getRobot().getCoords(), imaginaryTopEnemyGoal[0], imaginaryTopEnemyGoal[1]);
			if (!intersection.equals(null)){
				if (intersection.y > imaginaryTopEnemyGoal[0].y && intersection.y < imaginaryTopEnemyGoal[1].y){
					boolean clear = Utilities.isPathClear(getBallCoords(),intersection, getEnemyRobot());
					//System.out.println("intersection "+Vector2D.add(new Vector2D(intersection),new Vector2D(new Point2D.Double(0,Utilities.PITCH_HEIGHT_CM))));
					if (clear)
						return true;
				}
			}	
		}
		else {//robot in lower half of pitch, play ball with bottom wall
			Point2D.Double intersection = Utilities.intersection(getBallCoords(), getRobot().getCoords(), imaginaryBottomEnemyGoal[0], imaginaryBottomEnemyGoal[1]);
			if (!intersection.equals(null)){
				if (intersection.y > imaginaryBottomEnemyGoal[0].y && intersection.y < imaginaryBottomEnemyGoal[1].y){
					boolean clear = Utilities.isPathClear(getBallCoords(),intersection, getEnemyRobot());
					//System.out.println("intersection "+Vector2D.add(new Vector2D(intersection),new Vector2D(new Point2D.Double(0,-Utilities.PITCH_HEIGHT_CM))));
					if (clear)
						return true;
				}
			}	
		}

		return false;
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
	 * Returns the point the robot should go to behind the ball.
	 * Distance behind the ball set by POINT_OFFSET
	 * @param point The target point on the goal the robot should be aligned to.
	 * @return Point2D.Double behind the ball
	 */
	public Point2D.Double getPointBehindBall(Point2D.Double point) {
		Point2D.Double ball = getBallCoords();

		if (point.getY() == ball.getY()) {
			return new Point2D.Double(my_goal_left ? ball.getX() - POINT_OFFSET : ball.getX() + POINT_OFFSET, ball.getY());
		} else {
			double x, y, a, b;
			a = point.getY() - ball.getY();
			b = point.getX() - ball.getX();

			if (my_goal_left) {
				y = ball.getY() - POINT_OFFSET*a/(Math.sqrt(b*b + a*a));
				x = ball.getX() + (b*(y - ball.getY())/a);
			} else {
				y = ball.getY() + POINT_OFFSET*a/(Math.sqrt(b*b + a*a));
				x = ball.getX() - (b*(y - ball.getY())/a);
			}

			//x = ball.getX() + (b*(y - ball.getY())/a);

			return new Point2D.Double(x,y);
		}
	}


	/**
	 * Calculates all the points behind the ball that would align the robot to shoot and
	 * returns the point closest to the robot.
	 * @return The point closest to the robot that would allow it to shoot.
	 * @throws NullPointerException Throws exception when the robot can't see a goal.
	 */
	public Point2D.Double getOptimalPointBehindBall() throws NullPointerException {
		ArrayList<Point2D.Double> goal_points = new ArrayList<Point2D.Double>();
		Point2D.Double min_point = null;
		double min_distance = Utilities.PITCH_WIDTH_CM*2;

		goal_points.add(enemy_goal.getCentre());
		goal_points.add(enemy_goal.getTop());
		goal_points.add(enemy_goal.getBottom());
		goal_points.add(new Point2D.Double(enemy_goal.getCentre().x, enemy_goal.getCentre().y - Utilities.PITCH_HEIGHT_CM));
		goal_points.add(new Point2D.Double(enemy_goal.getCentre().x, enemy_goal.getCentre().y + Utilities.PITCH_HEIGHT_CM));

		Iterator<Point2D.Double> itr = goal_points.iterator();
		while (itr.hasNext()) {
			Point2D.Double point = itr.next();
			if (!Utilities.isPathClear(point, getBallCoords(), enemy_robot)) {
				itr.remove();
			}
		}

		itr = goal_points.iterator();
		while (itr.hasNext()) {
			Point2D.Double point = itr.next();
			Point2D.Double temp_point = getPointBehindBall(point);
			
			if (Utilities.isPointInField(temp_point)) { 
				if (!isPointInRobot(temp_point)) {
					//System.out.println(Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength());
					//System.out.println("Min distance: "+min_distance);
					if (Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength() < min_distance) {
						min_point = temp_point;
						min_distance = Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength();
					}
				}
			}
		}
		
		//System.out.println(min_point);

		return min_point;
	}
	
	/**
	 * Helper function to find if a specific point is within the enemy robot.
	 * @param point The point to check.
	 * @return Returns true if the point is within the enemy robot.
	 */
	public boolean isPointInRobot(Point2D.Double point) {
		if (Utilities.pointInTriangle(point, enemy_robot.getFrontLeft(), enemy_robot.getFrontRight(), enemy_robot.getBackLeft()) && 
				Utilities.pointInTriangle(point, enemy_robot.getBackLeft(), enemy_robot.getBackRight(), enemy_robot.getFrontRight())){
			return true;
		}
		return false;
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
