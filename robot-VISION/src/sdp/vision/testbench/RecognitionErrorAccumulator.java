package sdp.vision.testbench;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import sdp.common.WorldState;
import sdp.vision.processing.ImageProcessorConfig;
import sdp.vision.processing.ProcUtils;


/**
 * Accumulates vision system recognition errors and provides various
 * data about them.
 * 
 * This class can report the average recognition distance for each of the
 * object locations and the number of invalid recognitions. A recoginition
 * record is said to be invalid if it finds an object when it is not there
 * and vice-versa. 
 * 
 * @author Aaron Cronin
 * @author Gediminas Liktaras
 */
public class RecognitionErrorAccumulator {
	
	/** Sum of all ball position errors. */
	private double totalBallPosError = 0.0;	
	/** Sum of all blue robot position errors. */
	private double totalBluePosError = 0.0;	
	/** Sum of all yellow robot position errors. */
	private double totalYellowPosError = 0.0;
	
	/** The number of valid ball records accumulated. */
	private int validBallRecordCount = 0;
	/** The number of valid blue robot records accumulated. */
	private int validBlueRecordCount = 0;
	/** The number of valid yellow robot records accumulated. */
	private int validYellowRecordCount = 0;

	/** A list with accumulated ball position errors. */
	private ArrayList<Double> ballPosErrors = new ArrayList<Double>();
	/** A list with accumulated blue robot position errors. */
	private ArrayList<Double> bluePosErrors = new ArrayList<Double>();
	/** A list with accumulated yellow robot position errors. */
	private ArrayList<Double> yellowPosErrors = new ArrayList<Double>();
	
	/** Image processor configuration to use. */
	private ImageProcessorConfig config;
	
	
	/**
	 * Create a new recognition error accumulator.
	 * 
	 * @param config Image processor configuration to use.
	 */
	public RecognitionErrorAccumulator(ImageProcessorConfig config) {
		this.config = config;
	}
	
	
	/**
	 * Add a recognition error entry.
	 * 
	 * @param expected Expected world state.
	 * @param actual Actual world state, as produced by the vision system.
	 */
	public void addRecord(WorldState expected, WorldState actual) {
		double ballError = computePositionError(expected.getBallCoords(),
				actual.getBallCoords());
		ballPosErrors.add(ballError);
		if (ballError >= 0.0) {
			totalBallPosError += ballError;
			++validBallRecordCount;
		}
		
		double blueError = computePositionError(expected.getBlueRobot().getCoords(),
				actual.getBlueRobot().getCoords());
		ballPosErrors.add(blueError);
		if (blueError >= 0.0) {
			totalBluePosError += blueError;
			++validBlueRecordCount;
		}
		
		double yellowError = computePositionError(expected.getYellowRobot().getCoords(),
				actual.getYellowRobot().getCoords());
		ballPosErrors.add(yellowError);
		if (yellowError >= 0.0) {
			totalYellowPosError += yellowError;
			++validBallRecordCount;
		}
	}
	
	/**
	 * Compute the position error.
	 * 
	 * If the measurement was invalid (object detected when there is none
	 * and vice-versa), -1.0 is returned instead.
	 * 
	 * @param expectedPos Expected position.
	 * @param actualPos Actually found position.
	 * @return Position error.
	 */
	private double computePositionError(Point2D.Double expectedPos, Point2D.Double actualPos) {
		int validFlag = 1;
		if ((expectedPos.x < 0) && (expectedPos.y < 0)) {
			validFlag *= -1;
		}
		if ((actualPos.x < 0) && (actualPos.y < 0)) {
			validFlag *= -1;
		}
		
		if (validFlag > 0) {
			double xDiff = Math.abs(expectedPos.x - actualPos.x);
			double yDiff = Math.abs(expectedPos.y - actualPos.y);
			//Point2D.Double pixelError = ProcUtils.normalToFrameCoordinates(config, xDiff, yDiff, true);
			Point2D.Double pixelError = new Point2D.Double(xDiff, yDiff);
			
			return Math.sqrt(pixelError.x * pixelError.x + pixelError.y * pixelError.y);
		} else {
			return -1.0;
		}
	}
	
	
	/**
	 * Get the list with ball position errors.
	 * 
	 * @return The list with ball position errors.
	 */
	public ArrayList<Double> getBallPosErrors() {
		return ballPosErrors;
	}

	/**
	 * Get the list with blue robot position errors.
	 * 
	 * @return The list with blue robot position errors.
	 */
	public ArrayList<Double> getBluePosErrors() {
		return bluePosErrors;
	}

	/**
	 * Get the list with the yellow robot position errors.
	 * 
	 * @return The list with the yellow robot position errors.
	 */
	public ArrayList<Double> getYellowPosErrors() {
		return yellowPosErrors;
	}
	
	
	/**
	 * Get the number of valid ball records.
	 * 
	 * @return The number of valid ball records.
	 */
	public int getValidBallRecordCount() {
		return validBallRecordCount;
	}

	/**
	 * Get the number of valid blue robot records.
	 * 
	 * @return The number of valid blue robot records.
	 */
	public int getValidBlueRecordCount() {
		return validBlueRecordCount;
	}

	/**
	 * Get the number of valid yellow robot records.
	 * 
	 * @return The number of valid yellow robot records.
	 */
	public int getValidYellowRecordCount() {
		return validYellowRecordCount;
	}
	
	
	/**
	 * Get the number of invalid ball records.
	 * 
	 * @return The number of invalid ball records.
	 */
	public int getInvalidBallRecordCount() {
		return ballPosErrors.size() - validBallRecordCount;
	}

	/**
	 * Get the number of invalid blue robot records.
	 * 
	 * @return The number of invalid blue robot records.
	 */
	public int getInvalidBlueRecordCount() {
		return bluePosErrors.size() - validBlueRecordCount;
	}

	/**
	 * Get the number of invalid yellow robot records.
	 * 
	 * @return The number of invalid yellow robot records.
	 */
	public int getInvalidYellowRecordCount() {
		return yellowPosErrors.size() - validYellowRecordCount;
	}
	
	
	/**
	 * Get the average ball position recognition error.
	 * 
	 * @return Average ball position error.
	 */
	public double averageBallPosError() {
		if (validBallRecordCount > 0) {
			return totalBallPosError / validBallRecordCount;
		} else {
			return -1.0;
		}
	}

	/**
	 * Get the average blue robot position recognition error.
	 * 
	 * @return Average blue robot position error.
	 */
	public double averageBluePosError() {
		if (validBlueRecordCount > 0) {
			return totalBluePosError / validBlueRecordCount;
		} else {
			return -1.0;
		}
	}
	
	/**
	 * Get the average yellow robot position recognition error.
	 * 
	 * @return Average yellow robot position error.
	 */
	public double averageYellowPosError() {
		if (validYellowRecordCount > 0) {
			return totalYellowPosError / validYellowRecordCount;
		} else {
			return -1.0;
		}
	}

}
