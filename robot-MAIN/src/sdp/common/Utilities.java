package sdp.common;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;


/**
 * Contains various utility methods, which do not fit anywhere else.
 */
public class Utilities {
	
	/** The double comparison accuracy that is required. */
	private static final double EPSILON = 1e-8;
	
	
	/**
	 * Checks whether two doubles have close enough values.
	 * 
	 * @param a First double to check.
	 * @param b Second double to check.
	 * @return Whether the values are equal enough.
	 */
	public static boolean areDoublesEqual(double a, double b) {
		return (Math.abs(a - b) < EPSILON);
	}
	
	
	/**
	 * Return a deep copy of the given BufferedImage.
	 * 
	 * Taken from
	 * http://stackoverflow.com/questions/3514158/how-do-you-clone-a-bufferedimage.
	 * 
	 * @param image BufferedImage to copy.
	 * @return A deep copy of image.
	 */
	public static BufferedImage deepBufferedImageCopy(BufferedImage image) {
		ColorModel cm = image.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = image.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}


	/**
	 * Convert java.awt.geom.Point2D to java.awt.Point.
	 * 
	 * @param pt Point2D to convert.
	 * @return Corresponding Point.
	 */
	public static Point pointFromPoint2D(Point2D pt) {
		return new Point((int)pt.getX(), (int)pt.getY());
	}


	/**
	 * Bind a double to the interval [Short.MIN_VALUE; Short.MAX_VALUE]. If
	 * the given value is outside this interval, it is set to the closer
	 * endpoint.
	 * 
	 * @param value A value.
	 * @return Restricted value, as described above.
	 */
	public static short restrictToShort(double value) {
		return restrictValueToInterval(value, Short.MIN_VALUE, Short.MAX_VALUE).shortValue();
	}

	
	/**
	 * Bind a short to the interval [-Robot.MAX_SPEED_CM_S; Robot.MAX_SPEED_CM_S].
	 * If the given value is outside this interval, it is set to the closer
	 * endpoint.
	 * 
	 * @param value A value.
	 * @return Restricted value, as described above.
	 */
	public static short restrictToRobotSpeed(short value) {
		return restrictValueToInterval(value, -Robot.MAX_DRIVING_SPEED, Robot.MAX_DRIVING_SPEED).shortValue();
	}
	
	
	/**
	 * Ensure that the given value is within the interval [lowerBound; upperBound].
	 * If the value is outside of this interval, the closer endpoint is returned.
	 * 
	 * @param value Value in question.
	 * @param lowerBound Lower bound of the interval.
	 * @param upperBound Upper bound of the interval.
	 * @return Restricted value, as described above.
	 */
	public static <T extends Number> T restrictValueToInterval(T value, T lowerBound, T upperBound) {
		if (value.longValue() < lowerBound.longValue()) {
			return lowerBound;
		} else if (value.longValue() > upperBound.longValue()) {
			return upperBound;
		} else {
			return value;
		}
	}
	

	/**
	 * Strip a string from whitespace.
	 * 
	 * Adapted from http://www.java2s.com/Code/Java/Data-Type/stripstring.htm.
	 * 
	 * @param string String to strip.
	 * @return Stripped string.
	 */
	public static String stripString(String string) {
		if ((string == null) || (string.length() == 0)) {
			return string;
		}

		int start = 0;
		while((start < string.length()) && Character.isWhitespace(string.charAt(start))) {
			start++;
		}

		int end = string.length();
		while((end > start) && Character.isWhitespace(string.charAt(end - 1))) {
			end--;
		}

		if (start == end) {
			return "";
		} else {
			return string.substring(start, end);
		}
	}
	
	
	/**
	 * Check whether the given value is within specified bounds.
	 * 
	 * If lower > upper, the function checks if the given value is within the
	 * (-INF; upper] OR [lower; +INF) interval.
	 * 
	 * @param value Value to check.
	 * @param lower Lower bound of the interval.
	 * @param upper Upper bound of the interval.
	 * @return Whether the value is within the specified interval.
	 */
	public static boolean valueWithinBounds(int value, int lower, int upper) {
		if (lower > upper) {
			return ((value >= lower) || (value <= upper));
		} else {
			return ((value >= lower) && (value <= upper));
		}
	}


	/**
	 * Pretty print an array to a string.
	 *
	 * @param array Array to print.
	 * @return String representation of the given array.
	 */
	public static String arrayToString(Object[] array) {
		if ((array == null) || (array.length == 0)) {
			return "[EMPTY]";
		} else if (array.length == 1) {
			return ("[" + array[0] + "]");
		} else {
			String ans = "[" + array[0];
			for (int i = 1; i < array.length; i++) {
				ans += "\t" + array[i];
			}
			ans += "]";
			return ans;
		}
	}

	
	/**
	 * Pretty print a boolean array to a string.
	 * 
	 * @param array An array to pretty print.
	 * @return String representation of the given array.
	 */
	public static String arrayToString(boolean[] array) {
		Boolean[] ans = new Boolean[array.length];
		for (int i = 0; i < ans.length; i++) {
			ans[i] = array[i];
		}
		
		return arrayToString(ans);
	}

	/**
	 * Pretty print a double array to a string.
	 * 
	 * @param array An array to pretty print.
	 * @return String representation of the given array.
	 */
	public static String arrayToString(double[] array) {
		Double[] ans = new Double[array.length];
		for (int i = 0; i < ans.length; i++) {
			ans[i] = array[i];
		}
		
		return arrayToString(ans);
	}
	
	/**
	 * Pretty print an integer array to a string.
	 * 
	 * @param array An array to pretty print.
	 * @return String representation of the given array.
	 */
	public static String arrayToString(int[] array) {
		Integer[] ans = new Integer[array.length];
		for (int i = 0; i < ans.length; i++) {
			ans[i] = array[i];
		}
		
		return arrayToString(ans);
	}

	
	/**
	 * Flatten an array of arrays.
	 * 
	 * @param arrays An array of arrays to flatten.
	 * @return Flattened array of arrays.
	 */
	public static double[] flattenArrays(double[]...arrays) {
		int sum = 0;
		for (int i = 0; i < arrays.length; i++) {
			sum += arrays[i].length;
		}
		double[] ans = new double[sum];
		
		int id = 0;
		for (int i = 0; i < arrays.length; i++) {
			for (int j = 0; j < arrays[i].length; j++) {
				ans[id] = arrays[i][j];
				if (ans[id] == Double.NaN) { 	// y u hear?
					ans[id] = 0;
				}
				id++;
			}
		}
		
		return ans;
	}

	
	/**
	 * Sweeps a scanning vector sensor between the given angles and returns the distance to the closest object. Angles are wrt to the current robot
	 * where angle 0 means forward, 180 or -180 means backwards, 90 means left, -90 means right of robot. <br/>
	 * The result could be interpreted as: <i>the distance to nearest obstacle in the specified region about the current robot</i>
	 * 
	 * TODO: Clean up, update docs, move somewhere more appropriate.
	 * 
	 * @param ws current world state
	 * @param am_i_blue true if my robot is blue, false if it is yellow
	 * @param start_angle the starting angle of the segment (the smallest arc will be selected)
	 * @param end_angle the ending angle of the segment (the smallest arc will be selected)
	 * @param scan_count how many parts the sector should be devided for scanning
	 * @return the vector distance to the closest collision point a.k.a. the minimum distance determined by the scanning vector which swept the sector scan_count times.
	 */
	public static Vector2D getSector(WorldState ws, boolean am_i_blue, double start_angle, double end_angle, int scan_count, boolean include_ball_as_obstacle) {
		start_angle = GeomUtils.normaliseAngle(start_angle);
		end_angle = GeomUtils.normaliseAngle(end_angle);
		final Robot me = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot();
		final Vector2D zero = Vector2D.ZERO();
		Double min_dist = null;
		Vector2D min_vec = null;
		final double sector_angle = GeomUtils.normaliseAngle(end_angle-start_angle);
		final double scan_angle = sector_angle/scan_count;
		for (double angle = start_angle; GeomUtils.normaliseAngle(end_angle-angle) * sector_angle >= 0; angle+=scan_angle) {
			final double ang_rad = angle*Math.PI/180d;
			final Vector2D distV = DeprecatedCode.raytraceVector(ws, me, zero, new Vector2D(-Math.cos(ang_rad), Math.sin(ang_rad)), am_i_blue, include_ball_as_obstacle);
			final double dist = distV.getLength();
			if (min_dist == null || dist < min_dist) {
				min_dist = dist;
				min_vec = distV;
			}
		}
		return min_vec;
	}

	/**
	 * TODO: Clean up, update docs, move somewhere more appropriate.
	 * 
	 * @param ws
	 * @param am_i_blue
	 * @param scan_count
	 * @param sector_count
	 * @param normalize_to_1
	 * @param include_ball_as_obstacle
	 * @return
	 */
	public static double[] getSectors(WorldState ws, boolean am_i_blue, int scan_count, int sector_count, boolean normalize_to_1, boolean include_ball_as_obstacle) {
		if (sector_count % 2 != 0 || (sector_count / 2) % 2 == 0) {
			System.out.println("Sectors must be even number which halves should be odd!");
			return null;
		}
		double[] ans = new double[sector_count];
		double sec_angle = 360d/sector_count;
		for (int i = 0; i < sector_count; i++)
			ans[i] = normalize_to_1 ?
					NNetTools.AI_normalizeDistanceTo1(getSector(ws, am_i_blue, GeomUtils.normaliseAngle(-90+i*sec_angle), GeomUtils.normaliseAngle(-90+(i+1)*sec_angle), scan_count, include_ball_as_obstacle), WorldState.PITCH_WIDTH_CM) :
						getSector(ws, am_i_blue, GeomUtils.normaliseAngle(-90+i*sec_angle), GeomUtils.normaliseAngle(-90+(i+1)*sec_angle), scan_count, include_ball_as_obstacle).getLength();
					return ans;
	}

	
	/**
	 * TODO: Document, clean up, move somewhere into the neural AI.
	 * 
	 * @param relative
	 * @param sector_count
	 * @return
	 */
	public static double[] getTargetInSectors(Vector2D relative, int sector_count) {
		if (sector_count % 2 != 0 || (sector_count / 2) % 2 == 0) {
			System.out.println("Sectors must be even number which halves should be odd!");
			return null;
		}
		double[] ans = new double[sector_count];
		double sec_angle = 360d/sector_count;
		for (int i = 0; i < sector_count; i++)
			ans[i] = NNetTools.AI_normalizeDistanceTo1(NNetTools.targetInSector(relative, GeomUtils.normaliseAngle(-90+i*sec_angle), GeomUtils.normaliseAngle(-90+(i+1)*sec_angle)), WorldState.PITCH_WIDTH_CM);
		return ans;
	}
}
