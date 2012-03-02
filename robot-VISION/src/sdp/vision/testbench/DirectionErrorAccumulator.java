package sdp.vision.testbench;

import java.util.ArrayList;


/**
 * An accumulator for direction error measurements.
 * 
 * Collects direction measurements, processes them and provides useful
 * information about them. Can report the average recognition distance,
 * the number of invalid, inaccurate and acceptable recognitions. A recognition
 * record is said to be invalid if the vision system finds an object when it
 * is not there and vice-versa.
 * 
 * @author Gediminas Liktaras
 */
public class DirectionErrorAccumulator {

	/** Maximum recognition error in degrees, before marking a measurement inaccurate. */
	public static final double DIR_ERROR_TOLERANCE = 20.0;


	/** The sum of all valid position measurement errors. */
	private double totalError = 0.0;
	/** The number of valid position measurements. */
	private int validRecordCount = 0;
	/** The number of accurate position measurements. */
	private int accurateRecordCount = 0;
	
	/** A list with accumulated position errors. */
	private ArrayList<Double> errorList = new ArrayList<Double>();
	
	
	/**
	 * Create a new direction error accumulator.
	 */
	public DirectionErrorAccumulator() { }
	
	
	/**
	 * Add a direction measurement record to the accumulator.
	 * 
	 * @param expectedDir Expected direction in degrees.
	 * @param actualDir Actual direction in degrees.
	 */
	public void addRecord(double expectedDir, double actualDir) {
		int validFlag = 1;
		if (expectedDir < 0) {
			validFlag *= -1;
		}
		if (actualDir < 0) {
			validFlag *= -1;
		}
		
		expectedDir = Math.toDegrees(expectedDir) + 179;
		expectedDir = (expectedDir + 90) % 360;
		
		double error = 0.0;
		if (validFlag > 0) {
			error = Math.abs(expectedDir - actualDir);
			error = Math.min(error, 360.0 - error);
		} else {
			error = -1.0;
		}
		
		errorList.add(error);
		if (error >= 0.0) {
			totalError += error;
			++validRecordCount;
			
			if (error <= DIR_ERROR_TOLERANCE) {
				++accurateRecordCount;
			}
		}
	}
	
	
	/**
	 * Get the given measurement error.
	 * 
	 * @param id Index of the error in question.
	 * @return The measurement error requested.
	 */
	public double getRecord(int id) {
		return errorList.get(id);
	}
	
	/**
	 * Get the number of records in the accumulator.
	 * 
	 * @return The number of records in the accumulator.
	 */
	public int getTotalRecordCount() {
		return errorList.size();
	}
	
	
	/**
	 * Get the number of valid records.
	 * 
	 * @return The number of valid records.
	 */
	public int getValidRecordCount() {
		return validRecordCount;
	}
	
	/**
	 * Get the number of invalid records.
	 * 
	 * @return The number of invalid records.
	 */
	public int getInvalidRecordCount() {
		return getTotalRecordCount() - getValidRecordCount();
	}
	
	
	/**
	 * Get the number of accurate records.
	 * 
	 * @return The number of accurate records.
	 */
	public int getAccurateRecordCount() {
		return accurateRecordCount;
	}
	
	/**
	 * Get the number of inaccurate records.
	 * 
	 * @return The number of inaccurate records.
	 */
	public int getInaccurateRecordCount() {
		return getValidRecordCount() - getAccurateRecordCount();
	}

	
	/**
	 * Get the average position measurement error.
	 * 
	 * @return Average position measurement error.
	 */
	public double averageError() {
		if (validRecordCount > 0) {
			return totalError / getValidRecordCount();
		} else {
			return -1.0;
		}
	}
}
