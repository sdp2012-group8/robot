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
	
	/** Initial prediction queue size. */
	private static final long PREDICT_FRAME_SPAN = 3;
	/** Maximum FPS in prediction logic. */
	private static final int PREDICTION_MAX_FPS = 25;
	/** Minimum FPS in prediction logic. */
	private static final int PREDICTION_MIN_FPS = 5;
	/** The time, by which the world state is advanced into the future, in ms. */
	private static final long PREDICTION_TIME = 400;
	
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
	
	/** Previous tick time, used in FPS calculations. */
	private long oldTime = -1;


	/**
	 * Create a new AI world state.
	 * 
	 * @param worldState World state to use as a basis.
	 * @param isOwnTeamBlue Whether our robot is in the blue team.
	 * @param isOwnGoalLeft Whether our goal is on the left side.
	 */
	public AIWorldState(WorldState worldState, boolean isOwnTeamBlue, boolean isOwnGoalLeft) {
		super(worldState.getBallCoords(), worldState.getBlueRobot(),
				worldState.getYellowRobot(), worldState.getWorldImage());
		update(worldState, isOwnTeamBlue, isOwnGoalLeft);
	}

	
	/**
	 * Update the world state.
	 * 
	 * @param worldState New world state to use as a basis.
	 * @param isOwnTeamBlue Whether our robot is in the blue team.
	 * @param isOwnGoalLeft Whether our goal is on the left side.
	 */
	public void update(WorldState worldState, boolean isOwnTeamBlue, boolean isOwnGoalLeft) {
		if (PREDICTION_ENABLED) {
			worldState = predictCurrentState(worldState);
		}		
		if (LOW_PASS_FILTER_ENABLED) {
			worldState = lowPass(this, worldState);
		}
		
		previousState = worldState;
		this.isOwnTeamBlue = isOwnTeamBlue;
		this.isOwnGoalLeft = isOwnGoalLeft;
		
		super.update(worldState.getBallCoords(), worldState.getBlueRobot(),
				worldState.getYellowRobot(), worldState.getWorldImage());

		if (isOwnGoalLeft) {
			ownGoal = getLeftGoal();
			enemyGoal = getRightGoal();
		} else {
			ownGoal = getRightGoal();
			enemyGoal = getLeftGoal();
		}

		if (isOwnTeamBlue) {
			ownRobot = getBlueRobot();
			enemyRobot = getYellowRobot();
		} else {
			ownRobot = (getYellowRobot());
			enemyRobot = (getBlueRobot());
		}
	}
	
	
	/**
	 * Attempt to mitigate camera lag by predicting actual current state from
	 * available information.
	 * 
	 * @param state Freshly observed world state.
	 * @return Predicted actual world state.
	 */
	private WorldState predictCurrentState(WorldState state) {
		// If the ball is not located, use ball position from the previous frame.
		if (previousState != null) {
			if (!state.isBallPresent()) {
				state = new WorldState(previousState.getBallCoords(), state.getBlueRobot(),
						state.getYellowRobot(), state.getWorldImage());
			}
		}
		
		// Update prediction queue.
		if (predictionQueue == null) {
			predictionQueue = new LinkedList<WorldState>();
			for (int i = 0; i < PREDICT_FRAME_SPAN; i++) {
				predictionQueue.add(state);
			}
		}
		predictionQueue.add(state);
		predictionQueue.poll();
		
		// Calculate current time offset.
		long fps = PREDICTION_MAX_FPS;
		if (oldTime != -1) {
			long curTime = System.currentTimeMillis();
			if (curTime != oldTime) {
				fps = (long) (1000 / (curTime - oldTime));
			}
		}		
		fps = Utilities.restrictValueToInterval(fps, PREDICTION_MIN_FPS, PREDICTION_MAX_FPS).longValue();
		oldTime = System.currentTimeMillis();

		// Run the simulator to find the predicted state.
		WorldState[] pqStates = predictionQueue.toArray(new WorldState[0]);
		WorldState predictedState = Simulator.simulateWs(PREDICTION_TIME, (int) fps,
				pqStates, true, ownLastCommand, isOwnTeamBlue);
		
		// If the ball is still not found, assume that it is in front of the robot.
		if (!state.isBallPresent()) {
			Robot ownRobot = (isOwnTeamBlue ? predictedState.getBlueRobot() : predictedState.getYellowRobot());
			predictedState = new WorldState(ownRobot.getFrontCenter(), predictedState.getBlueRobot(),
					predictedState.getYellowRobot(), predictedState.getWorldImage());
		}
		
		return predictedState;
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
		return GeomUtils.pointDistance(ownRobot.getFrontCenter(), getBallCoords());
	}

	public double getDistanceToGoal() {
		return GeomUtils.pointDistance(ownRobot.getCoords(), enemyGoal.getCentre());
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
