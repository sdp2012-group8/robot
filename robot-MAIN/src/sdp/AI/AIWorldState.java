package sdp.AI;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

import sdp.AI.AI.Command;
import sdp.common.Goal;
import sdp.common.Painter;
import sdp.common.Robot;
import sdp.common.Utilities;
import sdp.common.WorldState;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.simulator.Simulator;
import sdp.vision.processing.ImageProcessorConfig;

public class AIWorldState extends WorldState {
	
	private static final long PREDICTION_TIME = 400; // in ms
	private static final long PREDICT_FRAME_SPAN = 3;
	private Queue<WorldState> predict_queue = null;
	
	private WorldState last_st = null;

	private boolean my_team_blue;
	private boolean my_goal_left;
	private Goal enemy_goal;
	private Goal my_goal;

	//changing variables
	private Robot robot = null;
	private Robot enemy_robot = null;
	private double distance_to_ball;
	private double distance_to_goal;
	private double point_offset;
	
	// battery indicator
	private static final int BAT_TOP_OFF = 10;
	private static final int BAT_RIGHT_OFF = 50;
	private static final int BAT_WIDTH = 50;
	private static final int BAT_HEIGHT = 20;
	private static final int BAT_MIN_VOLT = 55;
	private static final int BAT_MAX_VOLT = 79;
	private static final int BAT_RED_BELOW_PER = 20;
	
	private Command command;
	
	private boolean left_sensor = false, right_sensor = false, dist_sensor = false;

	private int battery = -1;

	//flags
	boolean f_ball_on_field = false;

	public AIWorldState(WorldState world_state, boolean my_team_blue, boolean my_goal_left) {
		super(world_state.getBallCoords(), world_state.getBlueRobot(),world_state.getYellowRobot(), world_state.getWorldImage());

		update(world_state, my_team_blue, my_goal_left);
	}

	public void update(WorldState world_state, boolean my_team_blue, boolean my_goal_left) {		
		// To enable or disable the prediction uncomment/comment this line.
		world_state = predict(world_state, PREDICTION_TIME);
		
		// To enable or disable low pass, uncomment this line
		world_state = lowPass(this, world_state);
		
		last_st = world_state;
		
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


		distance_to_ball = GeomUtils.pointDistance(Utilities.getGlobalVector(robot, new Vector2D(Robot.LENGTH_CM/2, 0)), getBallCoords());
		distance_to_goal = GeomUtils.pointDistance(robot.getCoords(), enemy_goal.getCentre());

		// check and set flags
		if (getBallCoords() == new Point2D.Double(-1,-1)) {
			f_ball_on_field = false;
		}
	}
	
	private long fps = 25;
	private long oldtime = -1;
	
	private WorldState predict(WorldState input, long time_ms) {
		
		// handle ball going offscreen
		if (last_st != null) {
			if (input.getBallCoords().getX() == -244d || input.getBallCoords().getX() == -1d)
				input = new WorldState(last_st.getBallCoords(), input.getBlueRobot(), input.getYellowRobot(), input.getWorldImage());
		}
		
		if (predict_queue == null) {
			predict_queue = new LinkedList<WorldState>();
			for (int i = 0; i < PREDICT_FRAME_SPAN; i++)
				predict_queue.add(input);
		}
		
		predict_queue.add(input);
		predict_queue.poll();
		
		WorldState[] states = predict_queue.toArray(new WorldState[0]);
		
		if (oldtime != -1) {
			long currtime = System.currentTimeMillis();
			fps = (long) (1000d/(currtime - oldtime));
		}
		
		if (fps > 25)
			fps = 25;
		if (fps < 5)
			fps = 5;

		
		WorldState state = Simulator.simulateWs(time_ms, (int) fps,
				states,
				true, command, my_team_blue);
		
		oldtime = System.currentTimeMillis();
		
		return state;
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

		if (Utilities.lineIntersectsRobot(getBallCoords(), enemy_goal.getCentre(), enemy_robot)
				|| Utilities.lineIntersectsRobot(getBallCoords(), enemy_goal.getTop(), enemy_robot) 
				|| Utilities.lineIntersectsRobot(getBallCoords(), enemy_goal.getBottom(), enemy_robot)) {
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

		if (Math.abs(topmin) < Math.abs(midmin) && Math.abs(topmin) < Math.abs(botmin) && Utilities.lineIntersectsRobot(getBallCoords(), enemy_goal.getTop(), enemy_robot)) {
			return topmin;
		} else if (Math.abs(midmin) < Math.abs(botmin) && Utilities.lineIntersectsRobot(getBallCoords(), enemy_goal.getCentre(), enemy_robot)) {
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
		if (config != null) {
			p.setOffsets(config.getFieldLowX(), config.getFieldLowY(), config.getFieldWidth(), config.getFieldHeight());
		} else {
			p.setOffsets(0, 0, Simulator.IMAGE_WIDTH, Simulator.IMAGE_HEIGHT);
		}
		p.image(my_team_blue,my_goal_left);
		
		// battery status
		
		double batt_coeff = (battery-BAT_MIN_VOLT)/(double) (BAT_MAX_VOLT-BAT_MIN_VOLT);
		if (batt_coeff < 0)
			batt_coeff = 0;
		if (batt_coeff > 1)
			batt_coeff = 1;
		if (battery == -1)
			batt_coeff = 1;
		
		
		if (batt_coeff*100 < BAT_RED_BELOW_PER)
			p.g.setColor(new Color(255,150,150,220));
		else
			p.g.setColor(new Color(255,255,255,200));
		p.g.setStroke(new BasicStroke(2.0f));
		
		p.g.drawRoundRect(Simulator.IMAGE_WIDTH-BAT_WIDTH-BAT_RIGHT_OFF,
				BAT_TOP_OFF, BAT_WIDTH, BAT_HEIGHT, 5, 5);
		p.g.fillRoundRect(Simulator.IMAGE_WIDTH-BAT_RIGHT_OFF,
				BAT_TOP_OFF+BAT_HEIGHT/5, 4, BAT_HEIGHT-2*BAT_HEIGHT/5, 2, 2);
		
		if (batt_coeff*100 > 100-BAT_RED_BELOW_PER)
			p.g.setColor(new Color(150,255,150,220));
		
		p.g.fillRect(Simulator.IMAGE_WIDTH-BAT_WIDTH-BAT_RIGHT_OFF+2,
				BAT_TOP_OFF+2,
				(int) (batt_coeff*(BAT_WIDTH-3)), 
				BAT_HEIGHT-3);
		
	
		
		p.dispose();
	}
	
	////////////////////////////////////////////////////////
	// low pass filtering
	///////////////////////////////////////////////////////
	
	// this is the amount of filtering to be done
	// higher values mean that the new data will "weigh more"
	// so the more uncertainty in result, the smaller value you should use
	// don't use values less then 1!
	private double filteredPositionAmount = 0.8;
	private double filteredAngleAmount = 0.5;
	
	/**
	 * Low pass for angles
	 * @param old_value
	 * @param new_value
	 * @return the filtered angle
	 */
	private double lowPass(double old_value, double new_value, boolean angle) {
		if (Double.isNaN(new_value) || Double.isInfinite(new_value))
			return old_value;
		if (!angle)
			return (old_value+new_value*filteredPositionAmount)/((double) (filteredPositionAmount+1));
		else {
			Vector2D old_val = Vector2D.rotateVector(new Vector2D(1, 0), old_value);
			Vector2D new_val = Vector2D.rotateVector(new Vector2D(1, 0), new_value);
			Vector2D sum = Vector2D.add(old_val, Vector2D.multiply(new_val, filteredAngleAmount));
			Vector2D ans = Vector2D.divide(sum, filteredAngleAmount+1);
			return Vector2D.getDirection(ans);
		}
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
				lowPass(old_value.getX(), new_value.getX(), false),
				lowPass(old_value.getY(), new_value.getY(), false));
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
				lowPass(old_value.getAngle(), new_value.getAngle(), true));
		a.setCoords(true);
		return a;
	}
	
	private WorldState lowPass(WorldState old_state, WorldState new_state) {
		return new WorldState(lowPass(old_state.getBallCoords(), new_state.getBallCoords()),
				lowPass(old_state.getBlueRobot(), new_state.getBlueRobot()),
				lowPass(old_state.getYellowRobot(), new_state.getYellowRobot()),
				new_state.getWorldImage());
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
	
	public boolean isLeft_sensor() {
		return left_sensor;
	}

	public void setLeft_sensor(boolean left_sensor) {
		this.left_sensor = left_sensor;
	}

	public boolean isRight_sensor() {
		return right_sensor;
	}

	public void setRight_sensor(boolean right_sensor) {
		this.right_sensor = right_sensor;
	}

	public boolean isDist_sensor() {
		return dist_sensor;
	}

	public void setDist_sensor(boolean dist_sensor) {
		this.dist_sensor = dist_sensor;
	}

	public int getBattery() {
		return battery;
	}

	public void setBattery(int battery) {
		this.battery = battery;
	}
	
	public void setCommand(Command com) {
		command = com;
	}
}
