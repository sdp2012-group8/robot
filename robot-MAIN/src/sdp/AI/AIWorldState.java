package sdp.AI;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

import sdp.AI.Command;
import sdp.common.Painter;
import sdp.common.Utilities;
import sdp.common.geometry.GeomUtils;
import sdp.common.world.Goal;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;
import sdp.simulator.Simulator;
import sdp.simulator.SimulatorOld;
import sdp.simulator.SimulatorPhysicsEngine;
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
	private static final long PREDICTION_TIME = 200;
	
	/** Low pass filter new angle data weight. */
	private static final double LPF_ANGLE_WEIGHT = 0.5;
	/** Low pass filter new position data weight. */
	private static final double LPF_COORD_WEIGHT = 0.8;
	
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
	
	private static final boolean USE_NEW_SIMULATOR_FOR_PREDICTION = true;
	private final Simulator newSim;
	
	
	/** The low pass filter. */
	private static LowPassFilter lowPassFilter = new LowPassFilter(LPF_ANGLE_WEIGHT, LPF_COORD_WEIGHT);
	
	
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
		
		newSim = USE_NEW_SIMULATOR_FOR_PREDICTION ? new SimulatorPhysicsEngine(false, false) : null;
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
		WorldState predictedState = Simulator.simulateWs(USE_NEW_SIMULATOR_FOR_PREDICTION ? newSim : new SimulatorOld(false, 0.000001), PREDICTION_TIME, (int) fps,
				pqStates, true, ownLastCommand, isOwnTeamBlue);
		
		if (!state.isBallPresent()) {
			Robot ownPredictedRobot = (isOwnTeamBlue ? predictedState.getBlueRobot() : predictedState.getYellowRobot());
			if (GeomUtils.pointDistance(state.getBallCoords(), ownPredictedRobot.getCoords()) < 30) {
				predictedState = new WorldState(ownPredictedRobot.getFrontCenter(), predictedState.getBlueRobot(),
						predictedState.getYellowRobot(), predictedState.getWorldImage());
			}
		}
		
		if (GeomUtils.pointDistance(state.getBallCoords(), predictedState.getBallCoords()) > 60) {
			predictedState = new WorldState(state.getBallCoords(), predictedState.getBlueRobot(),
						predictedState.getYellowRobot(), predictedState.getWorldImage());
		}
		
		return predictedState;
	}
	
	
	/**
	 * Get whether our goal is on the left.
	 * 
	 * @return Whether our goal is on the left.
	 */
	public boolean isOwnGoalLeft() {
		return isOwnGoalLeft;
	}

	/**
	 * Get whether our robot's colour is blue.
	 * 
	 * @return Whether our colour is blue.
	 */
	public boolean isOwnTeamBlue() {
		return isOwnTeamBlue;
	}
	
	
	/**
	 * Get our robot.
	 * 
	 * @return Our robot.
	 */
	public Robot getOwnRobot() {
		return ownRobot;
	}
	
	/**
	 * Get the enemy robot.
	 * 
	 * @return The enemy robot.
	 */
	public Robot getEnemyRobot() {
		return enemyRobot;
	}

	
	/**
	 * Get our own goal.
	 * 
	 * @return Own goal.
	 */
	public Goal getOwnGoal() {
		return ownGoal;
	}
	
	/**
	 * Get the enemy goal.
	 * 
	 * @return The enemy goal.
	 */
	public Goal getEnemyGoal() {
		return enemyGoal;
	}


	/**
	 * Get whether the left sensor is reporting a collision.
	 * 
	 * @return Whether the left sensor is active.
	 */
	public boolean isLeftSensorActive() {
		return isLeftSensorActive;
	}

	/**
	 * Set whether the left sensor is reporting a collision.
	 * 
	 * @param active Whether the left sensor is active.
	 */
	public void setLeftSensorActive(boolean active) {
		this.isLeftSensorActive = active;
	}

	
	/**
	 * Get whether the right sensor is reporting a collision.
	 * 
	 * @return Whether the right sensor is active.
	 */
	public boolean isRightSensorActive() {
		return isRightSensorActive;
	}

	/**
	 * Set whether the right sensor is reporting a collision.
	 * 
	 * @param active Whether the right sensor is active.
	 */
	public void setRightSensorActive(boolean active) {
		this.isRightSensorActive = active;
	}

	
	/**
	 * Get whether the front sensor is reporting a collision.
	 * 
	 * @return Whether the front sensor is active.
	 */
	public boolean isFrontSensorActive() {
		return isFrontSensorActive;
	}

	/**
	 * Set whether the front sensor is reporting a collision.
	 * 
	 * @param active Whether the front sensor is active.
	 */
	public void setFrontSensorActive(boolean active) {
		this.isFrontSensorActive = active;
	}

	
	/**
	 * Get our robot's battery level.
	 * 
	 * @return Our robot's battery level.
	 */
	public int getBatteryLevel() {
		return batteryLevel;
	}

	/**
	 * Set our robot's battery level.
	 * 
	 * @param newLevel Our robot's new battery level.
	 */
	public void setBatteryLevel(int newLevel) {
		this.batteryLevel = newLevel;
	}
	
	
	/**
	 * Set our robot's last executed command.
	 * 
	 * @param lastCommand Our robot's last executed command.
	 */
	public void setCommand(Command lastCommand) {
		ownLastCommand = lastCommand;
	}
	
	
	/**
	 * Get the distance from the centre of our kicker to the ball.
	 * 
	 * @return Distance from the centre of our kicker to the ball.
	 */
	public double getDistanceToBall() {
		return GeomUtils.pointDistance(ownRobot.getFrontCenter(), getBallCoords());
	}

	/**
	 * Get the distance from the centre of our robot to the enemy goal.
	 * 
	 * @return Distance from the centre of our robot to the enemy goal.
	 */
	public double getDistanceToGoal() {
		return GeomUtils.pointDistance(ownRobot.getCoords(), enemyGoal.getCentre());
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
}
