package sdp.common;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import sdp.AI.AIWorldState;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;


/**
 * Contains various utility methods, which do not fit anywhere else.
 */
public class Utilities {
	
	/** Attack modes for optimal point calculations. */
	public enum AttackMode { DirectOnly, WallsOnly, Full };
	
	
	/** The double comparison accuracy that is required. */
	private static final double EPSILON = 1e-8;
	
	/** Size of the ball obstacle. */
	public static final double SIZE_OF_BALL_OBSTACLE = Robot.LENGTH_CM;
	
	/** How close the robot should be to the ball before it attempts to kick it. */
	public static final double KICKABLE_BALL_DIST = 6;
	/** How close the robot should be to the ball before it attempts to kick it. */
	public static final double ENEMY_KICKABLE_BALL_DIST = 20;
	
	/** The maximum attack angle for an attacking robot. */
	public static final double KICKABLE_ATTACK_ANGLE = 40.0;
	
	/** A flag that denotes that ball should be considered an obstacle. */
	public static final int BALL_IS_OBSTACLE_FLAG = 0x1;
	/** A flag that denotes that blue robot should be considered an obstacle. */
	public static final int BLUE_IS_OBSTACLE_FLAG = 0x2;
	/** A flag that denotes that yellow robot should be considered an obstacle. */
	public static final int YELLOW_IS_OBSTACLE_FLAG = 0x4;

	
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
	 * Ensure that the given angle in degrees is within the interval [-180; 180).
	 * 
	 * @param angle Angle, in degrees.
	 * @return Normalised angle, as described above.
	 */
	public static double normaliseAngle(double angle) {
		angle = angle % 360;
		if (angle > 180) {
			angle -= 360;
		}
		if (angle < -180) {
			angle += 360;
		}
		
		return angle;
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
	 * Calculates if the given point is within the field.
	 * Includes an offset equal to half of the length of the robot, to allow
	 * it to get behind the ball
	 * @param point The point to be checked
	 * @return True if the point is within the bounds of the field.
	 */
	public static boolean isPointInField(Point2D.Double point) {
		double offset = Robot.LENGTH_CM / 2;
		if (point.getX() >= offset && point.getX() <= (WorldState.PITCH_WIDTH_CM - offset)) {
			if (point.getY() >= offset && point.getY() <= (WorldState.PITCH_HEIGHT_CM - offset)) {
				return true;
			}
		}
		return false;
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

	public static boolean isLeft(Point2D.Double a, Point2D.Double b, Point2D.Double c){
		return ((b.x - a.x)*(c.y - a.y) - (b.y - a.y)*(c.x - a.x)) > 0;
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
		Vector2D localBall = Utilities.getLocalVector(worldState.getOwnRobot(), new Vector2D(worldState.getBallCoords()));
			
		// Check if the ball is within a certain range in front of us
		if (!(localBall.x < Robot.LENGTH_CM / 2 + KICKABLE_BALL_DIST && localBall.x > 0 && localBall.y < Robot.WIDTH_CM/2 && localBall.y > -Robot.WIDTH_CM/2)) {
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
		Vector2D robotToBallVec = Vector2D.subtract(new Vector2D(worldState.getBallCoords()), new Vector2D(worldState.getEnemyRobot().getCoords()));
		Vector2D ballToGoalVec = Vector2D.subtract(new Vector2D(worldState.getOwnGoal().getCentre()), new Vector2D(worldState.getBallCoords()));
		
		double attackAngle = robotToBallVec.getDirection() - ballToGoalVec.getDirection();
		attackAngle = normaliseAngle(attackAngle);
		
		if (robotToBallVec.getLength() > (ENEMY_KICKABLE_BALL_DIST + Robot.LENGTH_CM / 2)) {
			return false;
		}
		if (Math.abs(attackAngle) > KICKABLE_ATTACK_ANGLE) {
			return false;
		}
		if (!Utilities.lineIntersectsRobot(worldState.getOwnGoal().getCentre(), worldState.getBallCoords(), worldState.getOwnRobot())) {
			return false;
		}
		if (!Utilities.lineIntersectsRobot(worldState.getEnemyRobot().getCoords(), worldState.getEnemyRobot().getFrontCenter(), worldState.getOwnRobot())) {
			return false;
		}
		
		return true;
	}
	

	/**
	 * Calculate the optimal point in the current world state. The optimal point
	 * is a point that the robot should seek to go to next.
	 * 
	 * TODO: Move all logic out of deprecated functions here.
	 * 
	 * @param worldState Current world state.
	 * @param distToBall Desired distance to the ball.
	 * @param mode Robot's attack mode.
	 * @return Optimal point on the field.
	 */
	public static Point2D.Double getOptimalPoint(AIWorldState worldState,
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
	 * Check if the given point is inside the given robot.
	 * 
	 * @param point Point to check.
	 * @param robot Robot to check.
	 * @return If the point is inside the robot.
	 */
	public static boolean isPointInRobot(Point2D.Double point, Robot robot) {
		return GeomUtils.isPointInQuadrilateral(point, robot.getFrontLeft(),
				robot.getFrontRight(), robot.getBackRight(), robot.getBackLeft());
	}


	/**
	 * Check whether the given point is inside or in the vicinity of the
	 * given robot. Within vicinity means within half robot length to its side.
	 * 
	 * @param point Point of interest.
	 * @param robot Robot in question.
	 * @return Whether the point is around a robot.
	 */
	public static boolean isPointAroundRobot(Point2D.Double point, Robot robot){
		double offset = Robot.LENGTH_CM/2;
		double length = Robot.LENGTH_CM;
		double width = Robot.WIDTH_CM;
		double angle = robot.getAngle();

		Point2D.Double frontLeftPoint = GeomUtils.rotatePoint(new Point2D.Double(0, 0),
				new Point2D.Double(length / 2 + offset, width / 2 + offset), angle);
		GeomUtils.translatePoint(frontLeftPoint, robot.getCoords());

		Point2D.Double frontRightPoint = GeomUtils.rotatePoint(new Point2D.Double(0, 0),
				new Point2D.Double(length / 2 + offset, -width / 2 - offset), angle);
		GeomUtils.translatePoint(frontRightPoint, robot.getCoords());

		Point2D.Double backLeftPoint = GeomUtils.rotatePoint(new Point2D.Double(0, 0),
				new Point2D.Double(-length / 2 - offset, width / 2 + offset), angle);
		GeomUtils.translatePoint(backLeftPoint, robot.getCoords());

		Point2D.Double backRightPoint = GeomUtils.rotatePoint(new Point2D.Double(0, 0),
				new Point2D.Double(-length / 2 - offset, -width / 2 - offset), angle);
		GeomUtils.translatePoint(backRightPoint, robot.getCoords());
		
		return GeomUtils.isPointInQuadrilateral(point, frontLeftPoint, frontRightPoint,
				backRightPoint, backLeftPoint);
	}
	
	
	/**
	 * Checks whether coordinates of the given point are both negative.
	 * 
	 * @param point Point in question.
	 * @return Whether both x and y coordinates are negative.
	 */
	public static boolean isPointNegative(Point2D.Double point) {
		return ((point.x < 0) && (point.y < 0));
	}
	

	/**
	 * Returns the vector to the closest collision point in the world (wall or enemy)
	 * 
	 * @param ls current world in centimeters
	 * @param am_i_blue true if my robot is blue, false otherwise; prevents testing with itself
	 * @param point the point to be tested, usually a point inside the robot (more usually edges of my robot)
	 * @return the vector to the closest point when collision may occur
	 */
	public static Vector2D getNearestCollisionPoint(WorldState ls, boolean am_i_blue, Point2D.Double point) {
		return getNearestCollisionPoint(ls, am_i_blue, point, true);
	}

	/**
	 * Returns the vector to the closest collision point in the world (wall or enemy)
	 * 
	 * @param ls current world in centimeters
	 * @param am_i_blue true if my robot is blue, false otherwise; prevents testing with itself
	 * @param point the point to be tested, usually a point inside the robot (more usually edges of my robot)
	 * @param include_enemy whether to include enemy
	 * @return the vector to the closest point when collision may occur
	 */
	public static Vector2D getNearestCollisionPoint(WorldState ls, boolean am_i_blue, Point2D.Double point, boolean include_enemy) {
		Robot enemy = am_i_blue ? ls.getYellowRobot() : ls.getBlueRobot();
		Vector2D[] enemy_pts = new Vector2D[] {
				new Vector2D(enemy.getFrontLeft()),
				new Vector2D(enemy.getFrontRight()),
				new Vector2D(enemy.getBackLeft()),
				new Vector2D(enemy.getBackRight())
		};
		// top wall test
		Vector2D temp = Vector2D.subtract(new Vector2D(0, WorldState.PITCH_HEIGHT_CM), new Vector2D(0, point.getY()));
		Vector2D min = temp;
		// bottom wall test
		temp = Vector2D.subtract(new Vector2D(0, 0), new Vector2D(0, point.getY()));
		if (temp.getLength() < min.getLength())
			min = temp;
		// left wall test
		temp = Vector2D.subtract(new Vector2D(0, 0), new Vector2D(point.getX(), 0));
		if (temp.getLength() < min.getLength())
			min = temp;
		// right wall test
		temp = Vector2D.subtract(new Vector2D(WorldState.PITCH_WIDTH_CM, 0), new Vector2D(point.getX(), 0));
		if (temp.getLength() < min.getLength())
			min = temp;
		// closest distance to enemy
		if (include_enemy) {
			temp = closestDistance(enemy_pts, new Vector2D(point));
			if (temp.getLength() < min.getLength())
				min = temp;
		}
		// we have our point
		return min;
	}

	/**
	 * Return the distance to the closest point in the set
	 * @param pts set of points
	 * @param pt the point we are standing at
	 * @return the distance from my point to the closest one in the set
	 */
	private static Vector2D closestDistance(Vector2D[] pts, Vector2D pt) {
		Vector2D min = null;
		for (int i = 0; i < pts.length; i++) {
			Vector2D temp = Vector2D.subtract(pts[i], pt);
			if (min == null || temp.getLength() < min.getLength())
				min = temp;
		}
		return min;
	}



	/**
	 * Gets how many degrees should a robot turn in order to face a point
	 * Units don't matter as long as they are consistent.
	 * @param me
	 * @param point
	 * @return
	 */
	public static double getTurningAngle(Robot me, Vector2D point) {
		return Utilities.normaliseAngle(-me.getAngle()+Vector2D.getDirection(new Vector2D(-me.getCoords().getX()+point.getX(), -me.getCoords().getY()+point.getY())));
	}

	/**
	 * Transforms a vector from table coVector2D origin = getGlobalVector(robot, local_origin);
		Vector2D direction = Vector2D.subtract(origin, getGlobalVector(robot, local_direction));ordinates to robot coordinates
	 * @param me
	 * @param vector
	 * @return
	 */
	public static Vector2D getLocalVector(Robot me, Vector2D vector) {
		return Vector2D.rotateVector(Vector2D.subtract(vector, new Vector2D(me.getCoords())), -me.getAngle());
	}

	/**
	 * Converts local coordinate (generated by {@link #getLocalVector(Robot, Vector2D)}) to a table coordinate.
	 * @param me
	 * @param local
	 * @return
	 */
	public static Vector2D getGlobalVector(Robot me, Vector2D local) {
		return  Vector2D.add(
				Vector2D.rotateVector(local, me.getAngle()),
				new Vector2D(me.getCoords()));
	}
	
	
	/**
	 * Create an obstacle bitfield.
	 * 
	 * @param ballIsObstacle Whether the ball is an obstacle.
	 * @param blueIsObstacle Whether the blue robot is an obstacle.
	 * @param yellowIsObstacle Whether the yellow robot is an obstacle.
	 * @return Obstacle bitfield that matches the given parameter values.
	 */
	public static int makeObstacleFlags(boolean ballIsObstacle, boolean blueIsObstacle,
			boolean yellowIsObstacle) {
		int flags = 0;
		if (ballIsObstacle) {
			flags |= BALL_IS_OBSTACLE_FLAG;
		}
		if (blueIsObstacle) {
			flags |= BLUE_IS_OBSTACLE_FLAG;
		}
		if (yellowIsObstacle) {
			flags |= YELLOW_IS_OBSTACLE_FLAG;
		}
		
		return flags;
	}
	
	
	/**
	 * Create an obstacle bitfield to match our opponent robot.
	 * 
	 * @param ballIsObstacle Whether the ball is an obstacle.
	 * @param isOwnTeamBlue Whether our robot is blue.
	 * @return Obstacle bitfield that matches the opponent robot.
	 */
	public static int makeObstacleFlagsForOpponent(boolean ballIsObstacle,
			boolean isOwnTeamBlue) {
		return makeObstacleFlags(ballIsObstacle, !isOwnTeamBlue, isOwnTeamBlue);
	}


	/**
	 * Find the closest collision from the given point in the specified
	 * direction and return it as a vector. The vector's direction will match
	 * the given parameter and its length will match the distance to the first
	 * collision.
	 * 
	 * TODO: Move all raytracing functionality into this function.
	 * 
	 * @param state World state in which to perform the search.
	 * @param origin The point from which to cast the ray.
	 * @param direction Direction of the ray.
	 * @param obstacles A bitfield that is expected to contain any combination
	 * 		of flags BALL_IS_OBSTACLE_FLAG, BLUE_IS_OBSTACLE_FLAG and
	 * 		YELLOW_IS_OBSTACLE_FLAG. It denotes which objects are considered
	 * 		to be obstacles.
	 * @return Collision vector, as described above.
	 */
	public static Vector2D getClosestCollisionVec(WorldState state, Vector2D origin,
			Vector2D direction, int obstacles) {
		boolean ballIsObstacle = ((obstacles & BALL_IS_OBSTACLE_FLAG) != 0);
		
		Boolean robotCollision;
		if ((obstacles & BLUE_IS_OBSTACLE_FLAG) != 0) {
			if ((obstacles & YELLOW_IS_OBSTACLE_FLAG) != 0) {
				robotCollision = null;
			} else {
				robotCollision = false;
			}
		} else {
			robotCollision = true;
		}
		
		if (origin.getX() <= 0 || origin.getY() <= 0 || origin.getX() >= WorldState.PITCH_WIDTH_CM || origin.getY() >= WorldState.PITCH_HEIGHT_CM)
			return Vector2D.ZERO();
		Vector2D near;
		Vector2D temp = vectorLineIntersection(origin, direction, new Vector2D(0, 0), new Vector2D(WorldState.PITCH_WIDTH_CM, 0));
		near = temp;
		temp = vectorLineIntersection(origin, direction, new Vector2D(WorldState.PITCH_WIDTH_CM, 0), new Vector2D(WorldState.PITCH_WIDTH_CM, WorldState.PITCH_HEIGHT_CM));
		if (temp != null && (near == null || temp.getLength() < near.getLength()))
			near = temp;
		temp = vectorLineIntersection(origin, direction, new Vector2D(WorldState.PITCH_WIDTH_CM, WorldState.PITCH_HEIGHT_CM), new Vector2D(0, WorldState.PITCH_HEIGHT_CM));
		if (temp != null && (near == null || temp.getLength() < near.getLength()))
			near = temp;
		temp = vectorLineIntersection(origin, direction, new Vector2D(0, WorldState.PITCH_HEIGHT_CM), new Vector2D(0, 0));
		if (temp != null && (near == null || temp.getLength() < near.getLength()))
			near = temp;
		// collision with a Robot
		for (int i = 0; i <= 1; i++) {
			if (robotCollision != null && ((robotCollision ? 0 : 1) == i))
				continue;
			Robot robot = i == 0 ? state.getBlueRobot() : state.getYellowRobot();
			temp = vectorLineIntersection(origin, direction, new Vector2D(robot.getFrontLeft()), new Vector2D(robot.getFrontRight()));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
			temp = vectorLineIntersection(origin, direction, new Vector2D(robot.getFrontRight()), new Vector2D(robot.getBackRight()));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
			temp = vectorLineIntersection(origin, direction, new Vector2D(robot.getBackRight()), new Vector2D(robot.getBackLeft()));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
			temp = vectorLineIntersection(origin, direction, new Vector2D(robot.getBackLeft()), new Vector2D(robot.getFrontLeft()));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
		}
		// collision with ball
		if (ballIsObstacle) {
			Vector2D ball = new Vector2D(state.getBallCoords());
			temp = vectorLineIntersection(origin, direction, new Vector2D(ball.getX(), ball.getY()-SIZE_OF_BALL_OBSTACLE/2), new Vector2D(ball.getX(), ball.getY()+SIZE_OF_BALL_OBSTACLE/2));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
		}
		if (near != null) 
			return near;
		return
				Vector2D.changeLength(direction, WorldState.PITCH_WIDTH_CM);
	}
	
	/**
	 * Find the closest collision from the given point in the specified
	 * direction and return the distance to it.
	 * 
	 * @param state World state in which to perform the search.
	 * @param origin The point from which to cast the ray.
	 * @param direction Direction of the ray.
	 * @param obstacles A bitfield that is expected to contain any combination
	 * 		of flags BALL_IS_OBSTACLE_FLAG, BLUE_IS_OBSTACLE_FLAG and
	 * 		YELLOW_IS_OBSTACLE_FLAG. It denotes which objects are considered
	 * 		to be obstacles.
	 * @return Distance to the closest collision, as described above.
	 */
	public static double getClosestCollisionDist(WorldState state, Vector2D origin,
			Vector2D direction, int obstacles) {
		Vector2D collVec = getClosestCollisionVec(state, origin, direction, obstacles);
		return collVec.getLength();
	}
	
	
	/**
	 * Get the closest collision from a robot positioned in the starting
	 * point, looking into the direction of the specified point. Two values
	 * are reported: shortest collision of the left and the right sides of
	 * the robot.
	 * 
	 * Here is a "helpful" graphic:
	 * 
	 *  v- left side                                   /\ <- obstacle         |
	 * +----+--------------left collision vector----->[||]                    |
	 * |o |=| <- robot facing east                     \/       O <- target   |
	 * +----+--------------right collision vector---------------------------->|
	 *  ^- right side                                                 wall -> |
	 * 
	 * @param state Current world state.
	 * @param dirPt Point, in whose direction the check will be performed.
	 * @param widthFactor Factor by which the robot width we consider is
	 * 		modified.
	 * @param obstacles A bitfield that is expected to contain any combination
	 * 		of flags BALL_IS_OBSTACLE_FLAG, BLUE_IS_OBSTACLE_FLAG and
	 * 		YELLOW_IS_OBSTACLE_FLAG. It denotes which objects are considered
	 * 		to be obstacles.
	 * @return Left and right collision distances, as described above.
	 */
	public static Vector2D[] getClosestSideCollisions(WorldState state,
			Vector2D startPt, Vector2D dirPt, double widthFactor, int obstacles) {
		Vector2D dir = Vector2D.subtract(dirPt, startPt);
		double angle = (-dir.getDirection() + 90) * Math.PI / 180d;
		
		double factor = widthFactor * Robot.WIDTH_CM / 2;
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		
		Vector2D leftOffset = new Vector2D(cos * factor, sin * factor);
		Vector2D leftStartPt = Vector2D.add(startPt, leftOffset);
		Vector2D leftColl = getClosestCollisionVec(state, leftStartPt, dir, obstacles);
		
		Vector2D rightOffset = new Vector2D(-cos * factor, -sin * factor);
		Vector2D rightStartPt = Vector2D.add(startPt, rightOffset);
		Vector2D rightColl = getClosestCollisionVec(state, rightStartPt, dir, obstacles);
		
		Vector2D retValue[] = { leftColl, rightColl };
		return retValue;
	}
	
	
	/**
	 * Check whether a robot could drive between two points in a straight line
	 * without colliding with anything.
	 * 
	 * This function is pessimistic, since it considers a tunnel that is a bit
	 * wider than the robot.
	 * 
	 * @param state Current world state.
	 * @param point1 One of the path's endpoints.
	 * @param point2 Another of the path's endpoints.
	 * @param widthFactor Factor by which the robot width we consider is
	 * 		modified.
	 * @param obstacles A bitfield that is expected to contain any combination
	 * 		of flags BALL_IS_OBSTACLE_FLAG, BLUE_IS_OBSTACLE_FLAG and
	 * 		YELLOW_IS_OBSTACLE_FLAG. It denotes which objects are considered
	 * 		to be obstacles.
	 * @return Whether the path between two points is clear.
	 */
	public static boolean isDirectPathClear(WorldState state, Vector2D point1,
			Vector2D point2, int obstacles) {
		double pathLength = Vector2D.subtract(point2, point1).getLength();		
		double widthFactors[] = { 0.0, 0.25, 0.5, 0.75, 1.0, 1.25, 1.5 };
		
		for (double factor : widthFactors) {
			Vector2D sideColls[] = Utilities.getClosestSideCollisions(state, point1,
					point2, factor, obstacles);
			if ((sideColls[0].getLength() < pathLength)
					|| (sideColls[1].getLength() < pathLength)) {
				return false;
			}
		}
		
		return true;
	}
	

	/**
	 * Return the intersection of a vector in the given direction, originating from origin.
	 * @param origin
	 * @param direction
	 * @param lineStart
	 * @param lineEnd
	 * @return the direction vector with correct length if there is intersection, otherwise returns null
	 */
	public static Vector2D vectorLineIntersection(Vector2D origin, Vector2D direction, Vector2D lineStart, Vector2D lineEnd) {
		Vector2D loc_start = Vector2D.rotateVector(Vector2D.subtract(lineStart, new Vector2D(origin)), -Vector2D.getDirection(direction));
		Vector2D loc_end = Vector2D.rotateVector(Vector2D.subtract(lineEnd, new Vector2D(origin)), -Vector2D.getDirection(direction));
		if (loc_start.getX() < 0 && loc_end.getX() < 0)
			return null; // if the vector is facing the other way
		if (loc_start.getY() * loc_end.getY() <= 0 ) {
			// if there is intersection i.e. the local coordinates are on different sides of the local axis
			double length = loc_start.getX()-(loc_start.getY()*(loc_end.getX()-loc_start.getX())/(loc_end.getY()-loc_start.getY()));
			if (length < 0)
				return null;
			return Vector2D.changeLength(direction, length);
		} else
			return null;
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
		start_angle = Utilities.normaliseAngle(start_angle);
		end_angle = Utilities.normaliseAngle(end_angle);
		final Robot me = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot();
		final Vector2D zero = Vector2D.ZERO();
		Double min_dist = null;
		Vector2D min_vec = null;
		final double sector_angle = Utilities.normaliseAngle(end_angle-start_angle);
		final double scan_angle = sector_angle/scan_count;
		for (double angle = start_angle; Utilities.normaliseAngle(end_angle-angle) * sector_angle >= 0; angle+=scan_angle) {
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
					NNetTools.AI_normalizeDistanceTo1(getSector(ws, am_i_blue, Utilities.normaliseAngle(-90+i*sec_angle), Utilities.normaliseAngle(-90+(i+1)*sec_angle), scan_count, include_ball_as_obstacle), WorldState.PITCH_WIDTH_CM) :
						getSector(ws, am_i_blue, Utilities.normaliseAngle(-90+i*sec_angle), Utilities.normaliseAngle(-90+(i+1)*sec_angle), scan_count, include_ball_as_obstacle).getLength();
					return ans;
	}

	static double[] getTargetInSectors(Vector2D relative, int sector_count) {
		if (sector_count % 2 != 0 || (sector_count / 2) % 2 == 0) {
			System.out.println("Sectors must be even number which halves should be odd!");
			return null;
		}
		double[] ans = new double[sector_count];
		double sec_angle = 360d/sector_count;
		for (int i = 0; i < sector_count; i++)
			ans[i] = NNetTools.AI_normalizeDistanceTo1(NNetTools.targetInSector(relative, Utilities.normaliseAngle(-90+i*sec_angle), Utilities.normaliseAngle(-90+(i+1)*sec_angle)), WorldState.PITCH_WIDTH_CM);
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
