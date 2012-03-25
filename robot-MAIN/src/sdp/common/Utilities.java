package sdp.common;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import sdp.AI.AIWorldState;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;


/**
 * Contains various utility methods, which do not fit anywhere else.
 */
public class Utilities {
	
	/** Attack modes for optimal point calculations. */
	public enum AttackMode { DirectOnly, WallsOnly, Full };
	
	
	/** The double comparison accuracy that is required. */
	private static final double EPSILON = 1e-8;
	
	/** How close the robot should be to the ball before it attempts to kick it. */
	public static final double OWN_BALL_KICK_DIST = 6;
	/** How close the robot should be to the ball before it attempts to kick it. */
	public static final double ENEMY_BALL_KICK_DIST = Robot.LENGTH_CM;
	
	/** Distance threshold for ball-robot direction ray tests. */
	public static final double BALL_TO_DIRECTION_PROXIMITY_THRESHOLD = Robot.WIDTH_CM / 2;
	
	/** The maximum attack angle for an attacking robot. */
	public static final double KICKABLE_ATTACK_ANGLE = 40.0;
	
	
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
	 * Checks whether the given line intersects a robot.
	 * 
	 * @param point1 First point on the line.
	 * @param point2 Second point on the line.
	 * @param robot Robot in question.
	 * @return Whether the line segment in question intersects the robot.
	 */
	public static boolean lineIntersectsRobot(Point2D.Double point1, Point2D.Double point2,
			Robot robot) {
		boolean diagonal1 = GeomUtils.doesSegmentIntersectLine(robot.getBackLeft(),
				robot.getFrontRight(), point1, point2);
		boolean diagonal2 = GeomUtils.doesSegmentIntersectLine(robot.getFrontLeft(),
				robot.getBackRight(), point1, point2);
		return (diagonal1 && diagonal2);
	}


	/**
	 * Returns the point the robot should go to behind the ball.
	 * Distance behind the ball set by POINT_OFFSET
	 * @param point The target point on the goal the robot should be aligned to.
	 * @return Point2D.Double behind the ball
	 */
	public static Point2D.Double getPointBehindBall(Point2D.Double point, Point2D.Double ball, boolean my_goal_left, double point_offset) {

		if (point.getY() == ball.getY()) {
			return new Point2D.Double(my_goal_left ? ball.getX() - point_offset : ball.getX() + point_offset, ball.getY());
		} else {
			/*double x, y, a, b;
			a = point.getY() - ball.getY();
			b = point.getX() - ball.getX();

			if (my_goal_left) {
				y = ball.getY() - POINT_OFFSET*a/(Math.sqrt(b*b + a*a));
				x = ball.getX() + (b*(y - ball.getY())/a);
			} else {
				y = ball.getY() + POINT_OFFSET*a/(Math.sqrt(b*b + a*a));
				x = ball.getX() - (b*(y - ball.getY())/a);
			}*/
			
			Point2D.Double p = Vector2D.changeLength(Vector2D.subtract(new Vector2D(point),new Vector2D(ball)), -point_offset);
			p = new Point2D.Double(ball.x + p.x, ball.y + p.y);
			//x = ball.getX() + (b*(y - ball.getY())/a);

			return p;
		}
	}


	private static Point2D.Double toCentimeters(Point2D.Double original) {
		return new Point2D.Double(original.getX()*WorldState.PITCH_WIDTH_CM, original.getY()*WorldState.PITCH_WIDTH_CM);
	}

	private static Robot toCentimeters(Robot orig) {
		Robot robot = new Robot(toCentimeters(orig.getCoords()), orig.getAngle());
		robot.setCoords(true);
		return robot;
	}

	public static WorldState toCentimeters(WorldState orig) {
		return new WorldState(
				toCentimeters(orig.getBallCoords()),
				toCentimeters(orig.getBlueRobot()),
				toCentimeters(orig.getYellowRobot()),
				orig.getWorldImage());
	}
	
	
	/**
	 * Checks whether our robot can directly attack the enemy goal.
	 * 
	 * @param worldState Current world state.
	 * @return Whether the gates can be attacked.
	 */
	public static boolean canWeAttack(AIWorldState worldState) {
		Vector2D localBall = Robot.getLocalVector(worldState.getOwnRobot(), new Vector2D(worldState.getBallCoords()));
			
		// Check if the ball is within a certain range in front of us
		if (!(localBall.x < Robot.LENGTH_CM / 2 + OWN_BALL_KICK_DIST && localBall.x > 0 && localBall.y < Robot.WIDTH_CM/2 && localBall.y > -Robot.WIDTH_CM/2)) {
			return false;
		} else {
			System.out.println("I can kick da ball!!!!");
		}
		
		// Check there isn't a robot straight in front of us.
		if (!Utilities.lineIntersectsRobot(worldState.getOwnRobot().getCoords(), worldState.getOwnRobot().getFrontCenter(), worldState.getEnemyRobot())) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks whether enemy robot can directly attack our goal.
	 * 
	 * @param worldState Current world state.
	 * @return Whether the gates can be attacked.
	 */
	public static boolean canEnemyAttack(AIWorldState worldState) {
		// Check that the ball is between our goal and the enemy.
		if (worldState.isOwnGoalLeft()) {
			if (worldState.getEnemyRobot().getCoords().x < worldState.getBallCoords().x) {
				return false;
			}
		} else {
			if (worldState.getBallCoords().x < worldState.getEnemyRobot().getCoords().x) {
				return false;
			}
		}
		
		// Check that the enemy is pointing at us.
		double curEnemyAngle = worldState.getEnemyRobot().getAngle();
		double upperEnemyAngle = Vector2D.subtract(new Vector2D(worldState.getOwnGoal().getTop()),
				new Vector2D(worldState.getEnemyRobot().getCoords())).getDirection();
		double lowerEnemyAngle = Vector2D.subtract(new Vector2D(worldState.getOwnGoal().getBottom()),
				new Vector2D(worldState.getEnemyRobot().getCoords())).getDirection();
		
		double curToLower = GeomUtils.getAngleDifference(lowerEnemyAngle, curEnemyAngle);
		double curToUpper = GeomUtils.getAngleDifference(upperEnemyAngle, curEnemyAngle);
		double lowerToUpper = GeomUtils.getAngleDifference(lowerEnemyAngle, upperEnemyAngle);
		
		if (!Utilities.areDoublesEqual(curToLower + curToUpper, lowerToUpper)) {
			return false;
		}
		
		// Check that the enemy is in line with the ball.
		Point2D.Double ballRayPoint = GeomUtils.getClosestPointToLine(worldState.getBallCoords(), 
				worldState.getEnemyRobot().getCoords(), worldState.getEnemyRobot().getFrontCenter());
		double ballDistToRay = GeomUtils.pointDistance(worldState.getBallCoords(), ballRayPoint);
		
		if (ballDistToRay > BALL_TO_DIRECTION_PROXIMITY_THRESHOLD) {
			return false;
		}
		
		// Check that the enemy is within shooting distance.
		double enemyToBallDist = GeomUtils.pointDistance(worldState.getBallCoords(),
				worldState.getEnemyRobot().getCoords());
		
		if (enemyToBallDist > (ENEMY_BALL_KICK_DIST + Robot.LENGTH_CM / 2)) {
			return false;
		}
		
		return true;
	}
	

	/**
	 * Calculate the optimal attack point in the given world state. The robot
	 * should seek to move to the said point in order to make an attacking move.
	 * 
	 * TODO: Move all logic out of deprecated functions here.
	 * 
	 * @param worldState Current world state.
	 * @param distToBall Desired distance to the ball.
	 * @param mode Robot's attack mode.
	 * @return Optimal attack point.
	 */
	public static Point2D.Double getOptimalAttackPoint(AIWorldState worldState,
			double distToBall, AttackMode mode) {
		if (mode == AttackMode.DirectOnly) {
			return DeprecatedCode.getOptimalPointBehindBall(worldState, distToBall, false);
		} else if (mode == AttackMode.WallsOnly) {
			return DeprecatedCode.getOptimalPointBehindBall(worldState, distToBall, true);
		} else {	// mode == AttackMode.Full
			return DeprecatedCode.getOptimalPointBehindBall(worldState, distToBall);
		}
	}
	
	
	/**
	 * Calculate the optimal defence point in the given world state. The robot
	 * should seek to move to the said point in order to make a defensive move.
	 * 
	 * @param worldState Current world state.
	 * @return Optimal defence point.
	 */
	public static Point2D.Double getOptimalDefencePoint(AIWorldState worldState) {
		Vector2D ballVec = new Vector2D(worldState.getBallCoords());
		
		Vector2D ballGoalVec = Vector2D.subtract(new Vector2D(worldState.getOwnGoal().getCentre()), ballVec);
		Vector2D ballOffset = Vector2D.changeLength(ballGoalVec, Robot.LENGTH_CM);
		Vector2D ballPoint = Vector2D.add(ballVec, ballOffset);
		
		Vector2D closestPoint = new Vector2D(GeomUtils.getClosestPointToLine(worldState.getOwnRobot().getCoords(),
				new Vector2D(worldState.getOwnGoal().getCentre()), new Vector2D(worldState.getBallCoords())));
		
		double distToClosest = GeomUtils.pointDistance(worldState.getOwnRobot().getCoords(), closestPoint);
		
		Vector2D optimalPoint = ballPoint;
		
		double goalDir = ballGoalVec.getDirection();
		double closePtDir = Vector2D.subtract(closestPoint, ballPoint).getDirection();
		
		if (Utilities.areDoublesEqual(goalDir, closePtDir)) {
			optimalPoint = closestPoint;
			
			if (distToClosest < Robot.LENGTH_CM / 2) {
				optimalPoint = ballPoint;
			}
		}
		
		return optimalPoint;
	}
	

	/**
	 * Change in state of a robot
	 */
	private static double delta(Robot old_r, Robot new_r) {
		return old_r.getCoords().distance(new_r.getCoords())+Math.abs(new_r.getAngle()-old_r.getAngle());
	}

	/**
	 * Returns differences in two world states. If nothing changed a lot, the number would be very small
	 * 
	 * @param old_w
	 * @param new_w
	 * @return
	 */
	public static double delta(WorldState old_w, WorldState new_w) {
		return delta(old_w.getBlueRobot(), new_w.getBlueRobot())+
				delta(old_w.getYellowRobot(), new_w.getYellowRobot())+
				new_w.getBallCoords().distance(old_w.getBallCoords());
	}

	/**
	 * Just for printing arrays
	 * @param array
	 * @return
	 */
	public static String printArray(Object[] array) {
		if (array == null || array.length == 0)
			return "[EMPTY]";
		if (array.length == 1)
			return "["+array[0]+"]";
		String ans = "["+array[0];
		for (int i = 1; i < array.length; i++)
			ans=ans+"\t"+array[i];
		return ans+"]";
	}

	/**
	 * Just for printing arrays
	 * @param array
	 * @return
	 */
	public static String printArray(int[] array) {
		Integer[] ans = new Integer[array.length];
		for (int i = 0; i < ans.length; i++)
			ans[i] = array[i];
		return printArray(ans);
	}

	/**
	 * Just for printing arrays
	 * @param array
	 * @return
	 */
	public static String printArray(double[] array) {
		Double[] ans = new Double[array.length];
		for (int i = 0; i < ans.length; i++)
			ans[i] = array[i];
		return printArray(ans);
	}

	/**
	 * Join given arrays into one in the given order
	 * @param arrays
	 * @return
	 */
	public static double[] concat(double[]...arrays) {
		int sum = 0;
		for (int i = 0; i < arrays.length; i++)
			sum += arrays[i].length;
		double[] ans = new double[sum];
		int id = 0;
		for (int i = 0; i < arrays.length; i++)
			for (int j = 0; j < arrays[i].length; j++) {
				ans[id] = arrays[i][j];
				if (ans[id] == Double.NaN)
					ans[id] = 0;
				id++;
			}
		return ans;
	}

	/**
	 * Sweeps a scanning vector sensor between the given angles and returns the distance to the closest object. Angles are wrt to the current robot
	 * where angle 0 means forward, 180 or -180 means backwards, 90 means left, -90 means right of robot. <br/>
	 * The result could be interpreted as: <i>the distance to nearest obstacle in the specified region about the current robot</i>
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
	
	
	
	/**
	 * Returns an AIWorldState given the current world state plus some booleans.
	 * @param world_state
	 * @param my_team_blue
	 * @param my_goal_left
	 * @param do_prediction
	 * @return
	 */
	public static AIWorldState getAIWorldState(WorldState world_state, boolean my_team_blue, boolean my_goal_left) {
		return new AIWorldState(world_state, my_team_blue, my_goal_left);
	}
}
