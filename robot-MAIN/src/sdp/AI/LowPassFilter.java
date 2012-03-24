package sdp.AI;

import java.awt.geom.Point2D;

import sdp.common.Robot;
import sdp.common.WorldState;
import sdp.common.geometry.Vector2D;


/**
 * A low pass filter for smoothing data.
 */
public class LowPassFilter {
	
	/** Low pass filter new angle data weight. */
	private static final double DEFAULT_ANGLE_WEIGHT = 0.5;
	/** Low pass filter new position data weight. */
	private static final double DEFAULT_COORD_WEIGHT = 0.8;
	
	
	/** New angle value weight. */
	double angleWeight;
	/** New coordinate value weight. */
	double coordWeight;
	
	
	/**
	 * Create a new low pass filter with default weights.
	 */
	public LowPassFilter() {
		this(DEFAULT_ANGLE_WEIGHT, DEFAULT_COORD_WEIGHT);
	}
	
	/**
	 * Create a new low pass filter.
	 * 
	 * @param angleWeight New angle value weight.
	 * @param coordWeight New coordinate value weight.
	 */
	public LowPassFilter(double angleWeight, double coordWeight) {
		this.angleWeight = angleWeight;
		this.coordWeight = coordWeight;
	}

	
	/**
	 * Apply a low pass filter on angles.
	 * 
	 * @param oldAngle Old angle value.
	 * @param newAngle New angle value.
	 * @return Filtered angle value.
	 */
	public double applyOnAngles(double oldAngle, double newAngle) {
		if (Double.isNaN(newAngle) || Double.isInfinite(newAngle)) {
			return oldAngle;
		} else {
			Vector2D oldVec = Vector2D.rotateVector(new Vector2D(1, 0), oldAngle);
			Vector2D newVec = Vector2D.rotateVector(new Vector2D(1, 0), newAngle);
			Vector2D sum = Vector2D.add(oldVec, Vector2D.multiply(newVec, angleWeight));
			return sum.getDirection();
		}
	}
	
	/**
	 * Apply a low pass filter on coordinates.
	 * 
	 * @param oldCoord Old coordinate value.
	 * @param newCoord New coordinate value.
	 * @return Filtered coordinate value.
	 */
	public double applyOnCoords(double oldCoord, double newCoord) {
		if (Double.isNaN(newCoord) || Double.isInfinite(newCoord)) {
			return oldCoord;
		} else {
			return (oldCoord + newCoord * coordWeight) / ((double) (coordWeight + 1));
		}
	}
	
	
	/**
	 * Apply a low pass filter on 2D points.
	 * 
	 * @param oldPoint Old point value.
	 * @param newPoint New point value.
	 * @return Filtered point value.
	 */
	public Point2D.Double applyOnPoint2D(Point2D.Double oldPoint, Point2D.Double newPoint) {
		double lowX = applyOnCoords(oldPoint.getX(), newPoint.getX());
		double lowY = applyOnCoords(oldPoint.getY(), newPoint.getY());
		
		return new Point2D.Double(lowX, lowY);
	}

	/**
	 * Apply a low pass filter on a robot.
	 * 
	 * @param oldRobot Old robot.
	 * @param newRobot New robot.
	 * @return Filtered robot.
	 */
	public Robot applyOnRobot(Robot oldRobot, Robot newRobot) {
		Point2D.Double lowPos = applyOnPoint2D(oldRobot.getCoords(), newRobot.getCoords());
		double lowAngle = applyOnAngles(oldRobot.getAngle(), newRobot.getAngle());
		
		Robot lowRobot = new Robot(lowPos, lowAngle);
		lowRobot.setCoords(true);
		return lowRobot;
	}
	
	/**
	 * Apply a low pass filter on a world state.
	 * 
	 * @param oldState Old world state.
	 * @param newState New world state.
	 * @return Filtered world state.
	 */
	public WorldState applyOnWorldState(WorldState oldState, WorldState newState) {
		Point2D.Double lowBall = applyOnPoint2D(oldState.getBallCoords(), newState.getBallCoords());
		Robot lowBlue = applyOnRobot(oldState.getBlueRobot(), newState.getBlueRobot());
		Robot lowYellow = applyOnRobot(oldState.getYellowRobot(), newState.getYellowRobot());
		
		return new WorldState(lowBall, lowBlue, lowYellow, newState.getWorldImage());
	}
}
