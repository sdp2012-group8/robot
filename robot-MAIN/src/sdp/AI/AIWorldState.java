package sdp.AI;

import java.awt.BasicStroke;
import java.awt.Color;
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
import sdp.simulator.Simulator;
import sdp.vision.processing.ImageProcessorConfig;


/**
 * The state of the field, as seen by AIs.
 */
public class AIWorldState extends WorldState {
	
	/** Whether to draw the battery indicator. */
	private static final boolean BATTERY_INDICATOR_ENABLED = true;
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
	/** Percentage, at which the battery indicator becomes green. */
	private static final int BATTERY_HIGH_PERCENTAGE = 80;
	
	
	/** The low pass filter. */
	private static LowPassFilter lowPassFilter = new LowPassFilter();
	
	
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
			worldState = lowPassFilter.applyOnWorldState(this, worldState);
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
		if (previousState != null) {
			if (!state.isBallPresent()) {
				state = new WorldState(previousState.getBallCoords(), state.getBlueRobot(),
						state.getYellowRobot(), state.getWorldImage());
			}
		}
		
		if (predictionQueue == null) {
			predictionQueue = new LinkedList<WorldState>();
			for (int i = 0; i < PREDICT_FRAME_SPAN; i++) {
				predictionQueue.add(state);
			}
		}
		predictionQueue.add(state);
		predictionQueue.poll();
		
		long fps = PREDICTION_MAX_FPS;
		if (oldTime != -1) {
			long curTime = System.currentTimeMillis();
			if (curTime != oldTime) {
				fps = (long) (1000 / (curTime - oldTime));
			}
		}		
		fps = Utilities.restrictValueToInterval(fps, PREDICTION_MIN_FPS, PREDICTION_MAX_FPS).longValue();
		oldTime = System.currentTimeMillis();

		WorldState[] pqStates = predictionQueue.toArray(new WorldState[0]);
		WorldState predictedState = Simulator.simulateWs(PREDICTION_TIME, (int) fps,
				pqStates, true, ownLastCommand, isOwnTeamBlue);
		
		if (!state.isBallPresent()) {
			Robot ownRobot = (isOwnTeamBlue ? predictedState.getBlueRobot() : predictedState.getYellowRobot());
			predictedState = new WorldState(ownRobot.getFrontCenter(), predictedState.getBlueRobot(),
					predictedState.getYellowRobot(), predictedState.getWorldImage());
		}
		
		return predictedState;
	}

	
	/**
	 * Draw world state related information onto the given world image.
	 * 
	 * @param worldImage World image to modify.
	 * @param config Image processor configuration that is currently is use.
	 */
	public void onDraw(BufferedImage worldImage, ImageProcessorConfig config) {
		Painter p = new Painter(worldImage, this);
		if (config != null) {
			p.setOffsets(config.getFieldLowX(), config.getFieldLowY(),
					config.getFieldWidth(), config.getFieldHeight());
		} else {
			p.setOffsets(0, 0, Simulator.IMAGE_WIDTH, Simulator.IMAGE_HEIGHT);
		}
		p.image(isOwnTeamBlue, isOwnGoalLeft);
		
		if (BATTERY_INDICATOR_ENABLED) {
			double barRatioFull = 1.0;
			if (batteryLevel >= 0) {
				double voltageRange = BATTERY_MAX_VOLTAGE - BATTERY_MIN_VOLTAGE;
				barRatioFull = (batteryLevel - BATTERY_MIN_VOLTAGE) / voltageRange;
				barRatioFull = Utilities.restrictValueToInterval(barRatioFull, 0.0, 1.0).doubleValue();
			}
			
			if ((barRatioFull * 100) < BATTERY_LOW_PERCENTAGE) {
				p.g.setColor(new Color(255, 150, 150, 220));
			} else {
				p.g.setColor(new Color(255, 255, 255, 200));
			}
			p.g.setStroke(new BasicStroke(2.0f));
			
			p.g.drawRoundRect(Simulator.IMAGE_WIDTH - BATTERY_WIDTH - BATTERY_RIGHT_OFFSET,
					BATTERY_TOP_OFFSET, BATTERY_WIDTH, BATTERY_HEIGHT, 5, 5);
			p.g.fillRoundRect(Simulator.IMAGE_WIDTH - BATTERY_RIGHT_OFFSET,
					BATTERY_TOP_OFFSET + BATTERY_HEIGHT / 5, 4, BATTERY_HEIGHT - 2 * BATTERY_HEIGHT / 5,
					2, 2);
			
			if ((barRatioFull * 100) > BATTERY_HIGH_PERCENTAGE) {
				p.g.setColor(new Color(150, 255, 150, 220));
			}
			
			p.g.fillRect(Simulator.IMAGE_WIDTH - BATTERY_WIDTH - BATTERY_RIGHT_OFFSET + 2,
					BATTERY_TOP_OFFSET + 2,	(int) (barRatioFull * (BATTERY_WIDTH - 3)), 
					BATTERY_HEIGHT - 3);
		}
		
		p.dispose();
	}
	

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
