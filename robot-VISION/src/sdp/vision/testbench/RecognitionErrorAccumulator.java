package sdp.vision.testbench;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintStream;
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
	
	/** Maximum recognition error in pixels, before marking a measurement inaccurate. */
	private static final double POS_ERROR_TOLERANCE = 20.0;
	
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

	/** A list with accumulated test names. */
	private ArrayList<String> testNames = new ArrayList<String>();
	/** A list with accumulated ball position errors. */
	private ArrayList<Double> ballPosErrors = new ArrayList<Double>();
	/** A list with accumulated blue robot position errors. */
	private ArrayList<Double> bluePosErrors = new ArrayList<Double>();
	/** A list with accumulated yellow robot position errors. */
	private ArrayList<Double> yellowPosErrors = new ArrayList<Double>();
	
	
	/**
	 * Create a new recognition error accumulator.
	 * 
	 * @param config Image processor configuration to use.
	 */
	public RecognitionErrorAccumulator() { }
	
	
	/**
	 * Add a recognition error entry.
	 * 
	 * @param expected Expected world state *in frame coordinates*.
	 * @param actual Actual world state *in frame coordinates*.
	 */
	public void addRecord(String testName, WorldState expected, WorldState actual) {
		testNames.add(testName);
		
		double ballError = computePositionError(expected.getBallCoords(),
				actual.getBallCoords());
		ballPosErrors.add(ballError);
		if (ballError >= 0.0) {
			totalBallPosError += ballError;
			++validBallRecordCount;
		}
		
		System.err.println(expected.getBlueRobot().getCoords().x + " "
				+ actual.getBlueRobot().getCoords().x + " - "
				+ expected.getBlueRobot().getCoords().y + " "
				+ actual.getBlueRobot().getCoords().y);
		double blueError = computePositionError(expected.getBlueRobot().getCoords(),
				actual.getBlueRobot().getCoords());
		bluePosErrors.add(blueError);
		if (blueError >= 0.0) {
			totalBluePosError += blueError;
			++validBlueRecordCount;
		}
		
		double yellowError = computePositionError(expected.getYellowRobot().getCoords(),
				actual.getYellowRobot().getCoords());
		yellowPosErrors.add(yellowError);
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
			
			return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
		} else {
			return -1.0;
		}
	}
	
	
	/**
	 * Get the number of records in the accumulator.
	 * 
	 * @return The number of records in the accumulator.
	 */
	public int getTotalRecordCount() {
		return testNames.size();
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
	
	
	/**
	 * Print all accumulated metrics into the specified output stream.
	 * 
	 * @param out Output stream to dump to.
	 */
	public void dumpMetrics(PrintStream out) {
		out.format("=== TEST SUMMARY\n");
		out.format("\n");
		out.format("Number of tests: %d\n", testNames.size());
		out.format("Accuracy error tolerance: %.4f pixels.\n", POS_ERROR_TOLERANCE);
		out.format("\n");
		out.format("Measurement classification (invalid/inaccurate/ok):\n");
		out.format("  Ball position: %d/%d/%d\n", getInvalidBallRecordCount(),
				0, getValidBallRecordCount());
		out.format("  Blue robot position: %d/%d/%d\n", getInvalidBlueRecordCount(),
				0, getValidBlueRecordCount());
		out.format("  Yellow robot position: %d/%d/%d\n", getInvalidYellowRecordCount(),
				0, getValidYellowRecordCount());
		out.format("\n");
		out.format("Average ball position error: %.4f pixels.\n",
				averageBallPosError());
		out.format("Average blue robot position error: %.4f pixels.\n",
				averageBluePosError());
		out.format("Average yellow robot position error: %.4f pixels.\n",
				averageYellowPosError());
		
		out.format("\n");
		out.format("\n");
		out.format("=== INDIVIDUAL TEST DATA\n");
		for (int i = 0; i < testNames.size(); ++i) {
			out.format("\n");
			out.format("Test image: %s\n", testNames.get(i));
			out.format("  Ball position error: %.4f pixels - %s\n",
					ballPosErrors.get(i), posErrorMessage(ballPosErrors.get(i)));
			out.format("  Blue robot position error: %.4f pixels - %s\n",
					bluePosErrors.get(i), posErrorMessage(bluePosErrors.get(i)));
			out.format("  Yellow robot position error: %.4f pixels - %s\n",
					yellowPosErrors.get(i), posErrorMessage(yellowPosErrors.get(i)));
		}
	}
	
	/**
	 * Get an appropriate message for a position measurement error.
	 * 
	 * @param error Position measure error.
	 * @return A message for dumping.
	 */
	private String posErrorMessage(double error) {
		if (error < 0.0) {
			return "INVALID";
		} else if (error > POS_ERROR_TOLERANCE) {
			return "INACCURATE";
		} else {
			return "OK";
		}
	}

}
