package sdp.AI;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

import sdp.AI.Command;
import sdp.common.Goal;
import sdp.common.Painter;
import sdp.common.Robot;
import sdp.common.Utilities;
import sdp.common.WorldState;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.simulator.Simulator;
import sdp.vision.processing.ImageProcessorConfig;


/**
 * The state of the field, as seen by AIs.
 */
public class AIWorldState extends WorldState {
	
	/** Whether the low pass filter is enabled. */
	private static final boolean LOW_PASS_FILTER_ENABLED = true;
	/** Whether current state prediction is active. */
	private static final boolean PREDICTION_ENABLED = true;
	
	/** The time, by which the world state is advanced into the future, in ms. */
	private static final long PREDICTION_TIME = 400;
	/** Initial prediction queue size. */
	private static final long PREDICT_FRAME_SPAN = 3;
	
	/** Low pass filter new position data weight. Larger value -> new data has more weight. */
	private static final double LPF_FILTERED_POS_AMOUNT = 0.8;
	/** Low pass filter new angle data weight. Larger value -> new data has more weight. */
	private static final double LPF_FILTERED_ANGLE_AMOUNT = 0.5;
	
	/** Top offset of the battery level indicator. */
	private static final int BATTERY_TOP_OFFSET = 10;
	/** Right offset of the battery level indicator. */
	private static final int BATTERY_RIGHT_OFFSET = 50;
	/** Width of the battery level indicator. */
	private static final int BATTERY_WIDTH = 50;
	/** Height of the battery level indicator. */
	private static final int BATTERY_HEIGHT = 20;
	/** Minimum voltage the battery level indicator displays. */
	private static final int BATTERY_MIN_VOLTAGE = 70;
	/** Maximum voltage the battery level indicator displays. */
	private static final int BATTERY_MAX_VOLTAGE = 79;
	/** Percentage, at which the battery indicator becomes red. */
	private static final int BATTERY_LOW_PERCENTAGE = 20;
	
	
	/** A queue with past world states that are used in prediction. */
	private Queue<WorldState> predictionQueue = null;
	
	/** World state from the previous frame. */
	private WorldState previousState = null;
	 
	/** Whether our team is blue. */
	private boolean isOwnTeamBlue;
	/** Whether out goal is on the left. */
	private boolean isOwnGoalLeft;
	
	/** Reference to our robot. */
	private Robot ownRobot = null;
	/** Reference to our goal. */
	private Goal ownGoal;	
	/** Reference to enemy robot. */
	private Robot enemyRobot = null;
	/** Reference to enemy goal. */
	private Goal enemyGoal;
	
	/** The command our robot executed last. */
	private Command ownLastCommand;
	
	/** Whether the front ultrasound sensor is active. */
	private boolean isFrontSensorActive = false;
	/** Whether the left collision sensor is active. */
	private boolean isLeftSensorActive = false;
	/** Whether the right collision sensor is active. */
	private boolean isRightSensorActive = false;
	
	/** Robot's battery level. */
	private int batteryLevel = -1;
	
	/** Distance from the centre of our kicker to the ball. */
	private double ownDistanceToBall;
	/** Distance from the centre of our robot to the enemy goal. */
	private double ownDistanceToGoal;
	
	/** Previous tick time, used in FPS calculations. */
	private long oldTime = -1;


	public AIWorldState(WorldState world_state, boolean my_team_blue, boolean my_goal_left) {
		super(world_state.getBallCoords(), world_state.getBlueRobot(),world_state.getYellowRobot(), world_state.getWorldImage());

		update(world_state, my_team_blue, my_goal_left);
	}

	public void update(WorldState world_state, boolean my_team_blue, boolean my_goal_left) {
		if (PREDICTION_ENABLED) {
			world_state = predict(world_state, PREDICTION_TIME);
		}		
		if (LOW_PASS_FILTER_ENABLED) {
			world_state = lowPass(this, world_state);
		}
		
		previousState = world_state;
		
		this.isOwnTeamBlue = my_team_blue;
		this.isOwnGoalLeft = my_goal_left;

		if (my_goal_left) {
			enemyGoal = new Goal(new Point2D.Double(WorldState.PITCH_WIDTH_CM , WorldState.GOAL_CENTRE_Y ));
			ownGoal = new Goal(new Point2D.Double(0 , WorldState.GOAL_CENTRE_Y ));
		} else {
			enemyGoal = new Goal(new Point2D.Double(0 , WorldState.GOAL_CENTRE_Y ));
			ownGoal = new Goal(new Point2D.Double(WorldState.PITCH_WIDTH_CM , WorldState.GOAL_CENTRE_Y ));
		}

		super.update(world_state.getBallCoords(), world_state.getBlueRobot(),world_state.getYellowRobot(), world_state.getWorldImage());
		//update variables
		if (my_team_blue) {
			ownRobot = getBlueRobot();
			enemyRobot = getYellowRobot();
		} else {
			ownRobot = (getYellowRobot());
			enemyRobot = (getBlueRobot());
		}


		ownDistanceToBall = GeomUtils.pointDistance(Utilities.getGlobalVector(ownRobot, new Vector2D(Robot.LENGTH_CM/2, 0)), getBallCoords());
		ownDistanceToGoal = GeomUtils.pointDistance(ownRobot.getCoords(), enemyGoal.getCentre());

	}
	
	
	private WorldState predict(WorldState input, long time_ms) {
		
		// handle ball going offscreen5
		if (previousState != null) {
			if (input.getBallCoords().getX() == -244d || input.getBallCoords().getX() == -1d)
				input = new WorldState(previousState.getBallCoords(), input.getBlueRobot(), input.getYellowRobot(), input.getWorldImage());
		}
		
		if (predictionQueue == null) {
			predictionQueue = new LinkedList<WorldState>();
			for (int i = 0; i < PREDICT_FRAME_SPAN; i++)
				predictionQueue.add(input);
		}
		
		predictionQueue.add(input);
		predictionQueue.poll();
		
		WorldState[] states = predictionQueue.toArray(new WorldState[0]);
		
		long fps = 25;
		if (oldTime != -1) {
			long currtime = System.currentTimeMillis();
			fps = (long) (1000d/(currtime - oldTime));
		}
		
		if (fps > 25)
			fps = 25;
		if (fps < 5)
			fps = 5;

		
		boolean ball_visible = input.getBallCoords().getX() >= 0 && input.getBallCoords().getY() >= 0;
		
		WorldState state = Simulator.simulateWs(time_ms, (int) fps,
				states,
				true, ownLastCommand, isOwnTeamBlue);
		
		if (!ball_visible) {
			Robot my = isOwnTeamBlue ? state.getBlueRobot() : state.getYellowRobot();
			state = new WorldState(my.getFrontCenter(), state.getBlueRobot(), state.getYellowRobot(), state.getWorldImage());
		}
		
		oldTime = System.currentTimeMillis();
		
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

		enemyRobot.setCoords(true); //need to convert robot coords to cm

		if (Utilities.lineIntersectsRobot(getBallCoords(), enemyGoal.getCentre(), enemyRobot)
				|| Utilities.lineIntersectsRobot(getBallCoords(), enemyGoal.getTop(), enemyRobot) 
				|| Utilities.lineIntersectsRobot(getBallCoords(), enemyGoal.getBottom(), enemyRobot)) {
			return true;
		}

		//Check for "special" case where the ball is between the enemy robot and their goal.
		if (enemyGoal.getCentre().x == 0) {
			if (getBallCoords().x < enemyRobot.getCoords().x) return true;
		} else {
			if (getBallCoords().x > enemyRobot.getCoords().x) return true;
		}

		return false; //can't see goal
	}

	public double calculateShootAngle() {
		double topmin = anglebetween(getRobot().getCoords(), enemyGoal.getTop());
		double midmin = anglebetween(getRobot().getCoords(), enemyGoal.getCentre());
		double botmin = anglebetween(getRobot().getCoords(), enemyGoal.getBottom());

		if (Math.abs(topmin) < Math.abs(midmin) && Math.abs(topmin) < Math.abs(botmin) && Utilities.lineIntersectsRobot(getBallCoords(), enemyGoal.getTop(), enemyRobot)) {
			return topmin;
		} else if (Math.abs(midmin) < Math.abs(botmin) && Utilities.lineIntersectsRobot(getBallCoords(), enemyGoal.getCentre(), enemyRobot)) {
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
		p.image(isOwnTeamBlue,isOwnGoalLeft);
		
		// battery status
		
		double batt_coeff = (batteryLevel-BATTERY_MIN_VOLTAGE)/(double) (BATTERY_MAX_VOLTAGE-BATTERY_MIN_VOLTAGE);
		if (batt_coeff < 0)
			batt_coeff = 0;
		if (batt_coeff > 1)
			batt_coeff = 1;
		if (batteryLevel == -1)
			batt_coeff = 1;
		
		
		if (batt_coeff*100 < BATTERY_LOW_PERCENTAGE)
			p.g.setColor(new Color(255,150,150,220));
		else
			p.g.setColor(new Color(255,255,255,200));
		p.g.setStroke(new BasicStroke(2.0f));
		
		p.g.drawRoundRect(Simulator.IMAGE_WIDTH-BATTERY_WIDTH-BATTERY_RIGHT_OFFSET,
				BATTERY_TOP_OFFSET, BATTERY_WIDTH, BATTERY_HEIGHT, 5, 5);
		p.g.fillRoundRect(Simulator.IMAGE_WIDTH-BATTERY_RIGHT_OFFSET,
				BATTERY_TOP_OFFSET+BATTERY_HEIGHT/5, 4, BATTERY_HEIGHT-2*BATTERY_HEIGHT/5, 2, 2);
		
		if (batt_coeff*100 > 100-BATTERY_LOW_PERCENTAGE)
			p.g.setColor(new Color(150,255,150,220));
		
		p.g.fillRect(Simulator.IMAGE_WIDTH-BATTERY_WIDTH-BATTERY_RIGHT_OFFSET+2,
				BATTERY_TOP_OFFSET+2,
				(int) (batt_coeff*(BATTERY_WIDTH-3)), 
				BATTERY_HEIGHT-3);
		
	
		
		p.dispose();
	}
	
	////////////////////////////////////////////////////////
	// low pass filtering
	///////////////////////////////////////////////////////
	
	
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
			return (old_value+new_value*LPF_FILTERED_POS_AMOUNT)/((double) (LPF_FILTERED_POS_AMOUNT+1));
		else {
			Vector2D old_val = Vector2D.rotateVector(new Vector2D(1, 0), old_value);
			Vector2D new_val = Vector2D.rotateVector(new Vector2D(1, 0), new_value);
			Vector2D sum = Vector2D.add(old_val, Vector2D.multiply(new_val, LPF_FILTERED_ANGLE_AMOUNT));
			Vector2D ans = Vector2D.divide(sum, LPF_FILTERED_ANGLE_AMOUNT+1);
			return ans.getDirection();
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
		return enemyRobot;
	}

	public Robot getRobot() {
		return ownRobot;
	}

	public double getDistanceToBall() {
		return ownDistanceToBall;
	}

	public double getDistanceToGoal() {
		return ownDistanceToGoal;
	}

	public Goal getEnemyGoal() {
		return enemyGoal;
	}

	public Goal getMyGoal() {
		return ownGoal;
	}

	public boolean getMyGoalLeft() {
		return isOwnGoalLeft;
	}

	public boolean getMyTeamBlue() {
		return isOwnTeamBlue;
	}
	
	public boolean isLeft_sensor() {
		return isLeftSensorActive;
	}

	public void setLeft_sensor(boolean left_sensor) {
		this.isLeftSensorActive = left_sensor;
	}

	public boolean isRight_sensor() {
		return isRightSensorActive;
	}

	public void setRight_sensor(boolean right_sensor) {
		this.isRightSensorActive = right_sensor;
	}

	public boolean isDist_sensor() {
		return isFrontSensorActive;
	}

	public void setDist_sensor(boolean dist_sensor) {
		this.isFrontSensorActive = dist_sensor;
	}

	public int getBattery() {
		return batteryLevel;
	}

	public void setBattery(int battery) {
		this.batteryLevel = battery;
	}
	
	public void setCommand(Command com) {
		ownLastCommand = com;
	}
}
