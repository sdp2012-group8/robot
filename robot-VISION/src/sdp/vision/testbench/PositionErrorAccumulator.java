package sdp.vision.testbench;

import java.awt.geom.Point2D;
import java.util.ArrayList;


/**
 * An accumulator for position error measurements.
 * 
 * Collects position measurements, processes them and provides useful
 * information about them. Can report the average recognition distance,
 * the number of invalid, inaccurate and acceptable recognitions. A recognition
 * record is said to be invalid if the vision system finds an object when it
 * is not there and vice-versa.
 * 
 * @author Aaron Cronin
 * @author Gediminas Liktaras
 */
public class PositionErrorAccumulator {
	
	/** Maximum recognition error in pixels, before marking a measurement inaccurate. */
	public static final double POS_ERROR_TOLERANCE = 20.0;


	/** The sum of all valid position measurement errors. */
	private double totalError = 0.0;
	/** The number of valid position measurements. */
	private int validRecordCount = 0;
	/** The number of accurate position measurements. */
	private int accurateRecordCount = 0;
	
	/** A list with accumulated position errors. */
	private ArrayList<Double> errorList = new ArrayList<Double>();
	
	
	/**
	 * Create a new position error accumulator.
	 */
	public PositionErrorAccumulator() { }
	
	
	/**
	 * Add a position measurement record to the accumulator.
	 * 
	 * @param expectedPos Expected position in frame coordinates.
	 * @param actualPos Actual position in frame coordinates.
	 */
	public void addRecord(Point2D.Double expectedPos, Point2D.Double actualPos) {
		int validFlag = 1;
		if ((expectedPos.x < 0) || (expectedPos.y < 0)) {
			validFlag *= -1;
		}
		if ((actualPos.x < 0) || (actualPos.y < 0)) {
			validFlag *= -1;
		}
		
		double error = 0.0;
		if (validFlag < 0){
			System.out.print(expectedPos);
			System.out.print(actualPos);
			System.out.printf("\n");
		}
		if (validFlag > 0) {
			double xDiff = Math.abs(expectedPos.x - actualPos.x);
			double yDiff = Math.abs(expectedPos.y - actualPos.y);
			
			error = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
		} else {
			error = -1.0;
		}
		
		errorList.add(error);
		if (error >= 0.0) {
			totalError += error;
			++validRecordCount;
			
			if (error <= POS_ERROR_TOLERANCE) {
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
			return totalError / getTotalRecordCount();
		} else {
			return -1.0;
		}
	}
	
}
