package sdp.common;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Iterator;

import sdp.AI.AIWorldState;


/**
 * Contains various utility methods, which do not fit anywhere else.
 */
/**
 * @author mihaela_laura_ionescu
 *
 */
public class Utilities {


	private static final double POINT_OFFSET = 1.5*Robot.LENGTH_CM;
	public final static double SIZE_OF_BALL_OBSTACLE = Robot.LENGTH_CM;


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
	 * @param pt
	 * @return
	 */
	public static Point pointFromPoint2D(Point2D pt) {
		return new Point((int)pt.getX(), (int)pt.getY());
	}

	/**
	 * Rotate point p2 around point p1 by the given angle in degrees.
	 * 
	 * @param origin Rotation point.
	 * @param point Point to rotate.
	 * @param angle Angle of rotation in degrees.
	 * @return Rotated point.
	 */
	public static Point2D.Double rotatePoint(Point2D.Double origin,
			Point2D.Double point, double degrees)
	{
		double radAngle = Math.toRadians(degrees);

		double xDiff = point.x - origin.x;
		double yDiff = point.y - origin.y;

		double rotX = (xDiff * Math.cos(radAngle)) - (yDiff * Math.sin(-radAngle)) + origin.x;
		double rotY = (xDiff * Math.sin(-radAngle)) + (yDiff * Math.cos(radAngle)) + origin.y;

		return new Point2D.Double(rotX, rotY);
	}

	/**
	 * Rotate point p2 around point p1 by the given angle in degrees.
	 * 
	 * @param origin Rotation point.
	 * @param point Point to rotate.
	 * @param angle Angle of rotation in degrees.
	 * @return Rotated point.
	 */
	public static Point rotatePoint(Point origin, Point point, double angle) {
		Point2D.Double origin_f = new Point2D.Double(origin.x, origin.y);
		Point2D.Double point_f = new Point2D.Double(point.x, point.y);
		Point2D.Double retValue_f = rotatePoint(origin_f, point_f, angle);

		return new Point((int)retValue_f.x, (int)retValue_f.y);
	}


	/**
	 * Translate the given point by some offset.
	 * 
	 * @param point The point to translate.
	 * @param offset Translate offset.
	 */
	public static void translatePoint(Point2D.Double point, Point2D.Double offset) {
		point.x += offset.x;
		point.y += offset.y;
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
	 * Normalizes the given angle
	 * @param initial given angle in degrees
	 * @return normalized angle between -Pi and Pi
	 */
	public static double normaliseAngle(double initial) {
		initial = initial % 360;
		if (initial > 180)
			initial -= 360;
		if (initial < -180)
			initial += 360;
		return initial;
	}
	

	/**
	 * Calculates if a point p is within the triangle abc
	 * @param p Point in triangle
	 * @param a
	 * @param b
	 * @param c
	 * @return returns true if point is in triangle, false otherwise
	 */
	public static boolean pointInTriangle(Point2D.Double p, Point2D.Double a, Point2D.Double b, Point2D.Double c) {
		if (Utilities.sameSide(p,a, b,c) && Utilities.sameSide(p,b, a,c) && Utilities.sameSide(p,c, a,b)) {
			return true;
		} else {
			return false;
		}
		
	}


	/**
	 * Determines if a robot is in the way of the path AB
	 * @param A First point of line
	 * @param B Second point of line
	 * @param robot Robot to test against
	 * @return returns true if the path is clear
	 */
	public static boolean isPathClear(Point2D.Double A, Point2D.Double B, Robot robot) {
		boolean diagonal1 = Utilities.sameSide(robot.getBackLeft(), robot.getFrontRight(), A, B);
		boolean diagonal2 = Utilities.sameSide(robot.getFrontLeft(), robot.getBackRight(), A, B);
		return (diagonal1 && diagonal2);
		
		
	}


	/**
	 * Calculates the point of intersection between two lines given 4 points
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 * @return Returns the point of intersection or null if none exist
	 */
	public static Point2D.Double intersection(Point2D.Double a, Point2D.Double b, Point2D.Double c, Point2D.Double d) {
		double D = (a.x-b.x)*(c.y-d.y) - (a.y-b.y)*(c.x-d.x);
		if (D == 0) return null;
		double xi = ((c.x-d.x)*(a.x*b.y-a.y*b.x)-(a.x-b.x)*(c.x*d.y-c.y*d.x))/D;
		double yi = ((c.y-d.y)*(a.x*b.y-a.y*b.x)-(a.y-b.y)*(c.x*d.y-c.y*d.x))/D;

		return new Point2D.Double(xi,yi);
	}

	/**
	 * This checks if 2 points are on the same side of a segment
	 * Computes the cross product of P1A and AB and of P2A and AB
	 * If the dot product of the resulting vectors us >=0, they have the same direction
	 * @param p1
	 * @param p2
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean sameSide(Point2D.Double p1, Point2D.Double p2, Point2D.Double a, Point2D.Double b){
		Point3D cp1 = Utilities.crossProduct(Utilities.pointSubtract(b,a), Utilities.pointSubtract(p1,a));
		Point3D cp2 = Utilities.crossProduct(Utilities.pointSubtract(b,a), Utilities.pointSubtract(p2,a));
		if (Utilities.dotProduct(cp1, cp2) >= 0) {
			return true;
		} else {
			return false;
		}
	}


	public static Point3D crossProduct(Point2D.Double a, Point2D.Double b) {
		return Utilities.crossProduct(new Point3D(a.x,a.y,0), new Point3D(b.x,b.y,0));
	}


	public static Point3D crossProduct(Point3D a, Point3D b) {
		return new Point3D(a.y*b.z - a.z*b.y, a.x*b.z - a.z*b.x, a.x*b.y - a.y*b.x);
	}


	public static double dotProduct(Point3D a, Point3D b) {
		return a.x*b.x + a.y*b.y + a.z*b.z;
	}


	public static Point2D.Double pointSubtract(Point2D.Double a, Point2D.Double  b) {
		return new Point2D.Double(a.x-b.x, a.y-b.y);
	}


	/**
	 * Calculates the angle BAC
	 * @param A
	 * @param B
	 * @param C
	 * @return angle BAC
	 */
	public static double getAngle(Point2D.Double A, Point2D.Double B, Point2D.Double C) {
		double angle = 0d;
		double AB = getDistanceBetweenPoint(A,B);
		double AC = getDistanceBetweenPoint(A,C);
		double BC = getDistanceBetweenPoint(B,C);

		angle = Math.acos((AC*AC + AB*AB - BC*BC)/(2*AC*AB));

		return angle;
	}

	/**
	 * converts to a byte
	 * @param angle
	 * @return double between -128 and 127
	 */
	public static byte normaliseToByte(double angle){
		if (angle > 127)
			angle = 127;
		if (angle < -128)
			angle = -128;
		return (byte)angle;
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
			
			Point2D.Double p = Vector2D.change_length(Vector2D.subtract(new Vector2D(point),new Vector2D(ball)), -point_offset);
			p = new Point2D.Double(ball.x + p.x, ball.y + p.y);
			//x = ball.getX() + (b*(y - ball.getY())/a);

			return p;
		}
	}

	public static boolean isLeft(Point2D.Double a, Point2D.Double b, Point2D.Double c){
		return ((b.x - a.x)*(c.y - a.y) - (b.y - a.y)*(c.x - a.x)) > 0;
	}

	/**
	 * Calculates distance between two points.
	 * @param p1 start point
	 * @param p2 end point
	 * @return sqrt((x1-x2)^2+(y1-y2)^2)
	 */
	public static double getDistanceBetweenPoint(Point2D.Double p1, Point2D.Double p2)
	{
		return Math.sqrt((double)(Math.pow(p1.x-p2.x,2)+(Math.pow(p1.y-p2.y,2))));
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
	 * Calculates all the points behind the ball that would align the robot to shoot and
	 * returns the point closest to the robot.
	 * @return The point closest to the robot that would allow it to shoot.
	 * @throws NullPointerException Throws exception when the robot can't see a goal.
	 */
	public static Point2D.Double getOptimalPointBehindBall(WorldState ws, boolean my_goal_left, boolean my_team_blue, double point_offset) throws NullPointerException {
		Goal enemy_goal;
		Robot robot, enemy_robot;

		if (my_goal_left) {
			enemy_goal = new Goal(new Point2D.Double(WorldState.PITCH_WIDTH_CM , WorldState.GOAL_Y_CM ));
		} else {
			enemy_goal = new Goal(new Point2D.Double(0 , WorldState.GOAL_Y_CM ));
		}

		//update variables
		if (my_team_blue) {
			robot = ws.getBlueRobot();
			enemy_robot = ws.getYellowRobot();
		} else {
			robot = (ws.getYellowRobot());
			enemy_robot = (ws.getBlueRobot());
		}
		
		ArrayList<Point2D.Double> goal_points = new ArrayList<Point2D.Double>();
		Point2D.Double min_point = null;
		double min_distance = WorldState.PITCH_WIDTH_CM*2;

		goal_points.add(enemy_goal.getCentre());
		goal_points.add(enemy_goal.getTop());
		goal_points.add(enemy_goal.getBottom());
		goal_points.add(new Point2D.Double(enemy_goal.getCentre().x, enemy_goal.getCentre().y - WorldState.PITCH_HEIGHT_CM));
		goal_points.add(new Point2D.Double(enemy_goal.getCentre().x, enemy_goal.getCentre().y + WorldState.PITCH_HEIGHT_CM));

		Iterator<Point2D.Double> itr = goal_points.iterator();
		while (itr.hasNext()) {
			Point2D.Double point = itr.next();

			if (point.y < 0) {
				if (!Utilities.isPathClear(point, ws.getBallCoords(),
						enemy_robot.getTopImage())) {
					itr.remove();

				}
			} else if (point.y > WorldState.PITCH_HEIGHT_CM) {
				if (!Utilities.isPathClear(point, ws.getBallCoords(),
						enemy_robot.getBottomImage())) {
					itr.remove();

				}
			} else {

				if (!Utilities.isPathClear(point, ws.getBallCoords(),
						enemy_robot) || point.x > WorldState.PITCH_WIDTH_CM) {
					itr.remove();

				}

			}

		}

		//System.out.println("begin printing points");
	
		itr = goal_points.iterator();
		while (itr.hasNext()) {
			Point2D.Double point = itr.next();
			Point2D.Double temp_point = getPointBehindBall(point, ws.getBallCoords(),my_goal_left, point_offset);

			//System.out.println(temp_point);
			
			if (Utilities.isPointInField(temp_point)) { 
				if (!isPointAroundRobot(temp_point, enemy_robot) && isPathClear(temp_point, ws.getBallCoords(),
						enemy_robot)) {
					//System.out.println(Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength());
					//System.out.println("Min distance: "+min_distance);
					if (Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength() < min_distance) {
						min_point = temp_point;
						min_distance = Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength();
					}
				} 
			} 
		} 
		//System.out.println("min point "+min_point); //+ "enemy robot" + enemy_robot.getCoords() + "angle" + enemy_robot.getAngle() + "ball coords "+ws.getBallCoords());
		return min_point;
	}


	/**
	 * Calculates all the points behind the ball that would align the robot to shoot and
	 * returns the point closest to the robot.
	 * It does not take into account the image goals
	 * @return The point closest to the robot that would allow it to shoot.
	 * @throws NullPointerException Throws exception when the robot can't see a goal.
	 */
	public static Point2D.Double getOptimalPointBehindBallNoWalls(WorldState ws, boolean my_goal_left, boolean my_team_blue) throws NullPointerException {
		Goal enemy_goal;
		Robot robot, enemy_robot;

		if (my_goal_left) {
			enemy_goal = new Goal(new Point2D.Double(WorldState.PITCH_WIDTH_CM , WorldState.GOAL_Y_CM ));
		} else {
			enemy_goal = new Goal(new Point2D.Double(0 , WorldState.GOAL_Y_CM ));
		}

		//update variables
		if (my_team_blue) {
			robot = ws.getBlueRobot();
			enemy_robot = ws.getYellowRobot();
		} else {
			robot = (ws.getYellowRobot());
			enemy_robot = (ws.getBlueRobot());
		}
		ArrayList<Point2D.Double> goal_points = new ArrayList<Point2D.Double>();
		Point2D.Double min_point = null;
		double min_distance = WorldState.PITCH_WIDTH_CM*2;
		double offset = enemy_goal.getBottom().y - enemy_goal.getTop().y; //offset>0
		offset = offset/5;

		goal_points.add(enemy_goal.getCentre());
		goal_points.add(enemy_goal.getTop());
		goal_points.add(enemy_goal.getBottom());

		for (int i=0; i<4; i++)
			goal_points.add(new Point2D.Double(enemy_goal.getTop().x, enemy_goal.getTop().y - (i+1)*offset));

		Iterator<Point2D.Double> itr = goal_points.iterator();
		while (itr.hasNext()) {
			Point2D.Double point = itr.next();

			if (!Utilities.isPathClear(point, ws.getBallCoords(), enemy_robot)) 
				itr.remove();

		}

		itr = goal_points.iterator();
		while (itr.hasNext()) {
			Point2D.Double point = itr.next();
			Point2D.Double temp_point = getPointBehindBall(point, ws.getBallCoords(), my_goal_left, POINT_OFFSET);

			if (Utilities.isPointInField(temp_point)) { 
				if (!isPointAroundRobot(temp_point, enemy_robot)) {
					//System.out.println(Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength());
					//System.out.println("Min distance: "+min_distance);
					if (Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength() < min_distance) {
						min_point = temp_point;
						min_distance = Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength();
					}
				}
			}
		}


		return min_point;
	}	

	/**
	 * Helper function to find if a specific point is within the enemy robot.
	 * @param point The point to check.
	 * @return Returns true if the point is within the enemy robot.
	 */
	public static boolean isPointInRobot(Point2D.Double point, Robot enemy_robot) {
		if (Utilities.pointInTriangle(point, enemy_robot.getFrontLeft(), enemy_robot.getFrontRight(), enemy_robot.getBackLeft()) || 
				Utilities.pointInTriangle(point, enemy_robot.getBackLeft(), enemy_robot.getBackRight(), enemy_robot.getFrontRight())){
			return true;
		}
		return false;
	}


	/**
	 * Helper function to find if a specific point is around the enemy robot.
	 * @param point The point to check.
	 * @return Returns true if the point is within 1/3 of the length of the robot from the enemy robot.
	 */
	public static boolean isPointAroundRobot(Point2D.Double point, Robot enemy_robot){
		double offset = Robot.LENGTH_CM/2;
		double length = Robot.LENGTH_CM;
		double width = Robot.WIDTH_CM;
		double angle = enemy_robot.getAngle();

		Point2D.Double frontLeftPoint = rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(length / 2 + offset, width / 2 + offset), angle);
		translatePoint(frontLeftPoint, enemy_robot.getCoords());

		Point2D.Double frontRightPoint = rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(length / 2 + offset, -width / 2 - offset), angle);
		translatePoint(frontRightPoint, enemy_robot.getCoords());

		Point2D.Double backLeftPoint = rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(-length / 2 - offset, width / 2 + offset), angle);
		translatePoint(backLeftPoint, enemy_robot.getCoords());

		Point2D.Double backRightPoint = rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(-length / 2 - offset, -width / 2 - offset), angle);
		translatePoint(backRightPoint, enemy_robot.getCoords());

		if (pointInTriangle(point, frontLeftPoint, frontRightPoint, backLeftPoint) || 
				pointInTriangle(point, backLeftPoint, backRightPoint, frontRightPoint)){
			return true;
		}
		return false;	
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
	 * Transforms a vector from table coordinates to robot coordinates
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

	public static Vector2D getNearestCollisionPointFromMyPerspective(Robot me, Point2D.Double my_pos, WorldState worldState, boolean am_i_blue) {
		return getLocalVector(me, Vector2D.add(new Vector2D(my_pos), getNearestCollisionPoint(worldState, am_i_blue, new Vector2D(my_pos))));
	}


	/**
	 * Starting from origin in the given direction, find the first point of collision in the scene
	 * @param origin the start of the vector
	 * @param direction size doesn't matter, only the angle is relevant
	 * @param ignore_blue true to ignore blue robot, false to ignore yellow, null to include both
	 * @return a {@link Vector2D} in the same direction as direction but with greater length (distance from origin to the nearest collision point, raytraced along direction's direction)
	 */
	public static Vector2D raytraceVector(WorldState ws, Vector2D origin, Vector2D direction, Boolean ignore_blue, boolean include_ball_as_obstacle) {
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
			if (ignore_blue != null && ((ignore_blue ? 0 : 1) == i))
				continue;
			Robot robot = i == 0 ? ws.getBlueRobot() : ws.getYellowRobot();
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
		if (include_ball_as_obstacle) {
			Vector2D ball = new Vector2D(ws.getBallCoords());
			temp = vectorLineIntersection(origin, direction, new Vector2D(ball.getX(), ball.getY()-SIZE_OF_BALL_OBSTACLE/2), new Vector2D(ball.getX(), ball.getY()+SIZE_OF_BALL_OBSTACLE/2));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
		}
		if (near != null) 
			return near;
		return
				Vector2D.change_length(direction, WorldState.PITCH_WIDTH_CM);
	}

	/**
	 * Raytrace vector with relation to a robot
	 * @param ws
	 * @param robot
	 * @param local_origin In robot's coordinate system. Make sure it is outside robot to avoid false readings!
	 * @param local_direction In robot's coordinate system
	 * @return same output as {@link #raytraceVector(WorldState, Vector2D, Vector2D)} - in table coordinates
	 */
	public static Vector2D raytraceVector(WorldState ws, Robot robot, Vector2D local_origin, Vector2D local_direction, boolean include_ball_as_obstacle) {
		return raytraceVector(ws, robot, local_origin, local_direction, null, include_ball_as_obstacle);
	}


	/**
	 * Raytrace vector with relation to a robot
	 * @param ws
	 * @param robot
	 * @param local_origin In robot's coordinate system. Make sure it is outside robot to avoid false readings!
	 * @param local_direction In robot's coordinate system
	 * @param am_i_blue if true, ignores blue if false ignores yellow. To include all robots, use {@link #raytraceVector(WorldState, Robot, Vector2D, Vector2D)}
	 * @return same output as {@link #raytraceVector(WorldState, Vector2D, Vector2D)} - in table coordinates
	 */
	public static Vector2D raytraceVector(WorldState ws, Robot robot, Vector2D local_origin, Vector2D local_direction,  Boolean am_i_blue, boolean include_ball_as_obstacle) {
		Vector2D origin = getGlobalVector(robot, local_origin);
		Vector2D direction = Vector2D.subtract(origin, getGlobalVector(robot, local_direction));
		return raytraceVector(ws, origin, direction, am_i_blue, include_ball_as_obstacle);
	}

	/**
	 * Whether there is direct visibility from a point of the pitch to another one.
	 * @param ws
	 * @param robot
	 * @param startPt
	 * @param endPt
	 * @return
	 */
	public static boolean visibility(WorldState ws, Vector2D endPt, boolean am_i_blue, boolean include_ball_as_obstacle) {
		Robot robot = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot(); 
		Vector2D startPt = new Vector2D(robot.getCoords());
		Vector2D dir =  Vector2D.subtract(endPt, startPt);
		Vector2D ray = raytraceVector(ws, startPt, dir, am_i_blue, include_ball_as_obstacle);
		return ray.getLength() >= dir.getLength();
	}

	
	public static boolean visibility(WorldState ws, Vector2D startPoint, Vector2D endPt, boolean am_i_blue, boolean include_ball_as_obstacle) {
		
		Vector2D dir =  Vector2D.subtract(endPt, startPoint);
		Vector2D ray = raytraceVector(ws, startPoint, dir, am_i_blue, include_ball_as_obstacle);
		return ray.getLength() >= dir.getLength();
	}
	
	/**
	 * Returns the distance to the direct collision in a given direction.
	 * @param ws
	 * @param robot
	 * @param startPt
	 * @param endPt
	 * @return
	 */
	public static double visibility2(WorldState ws, Vector2D endPt, boolean am_i_blue, boolean include_ball_as_obstacle) {
		Robot robot = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot(); 
		Vector2D startPt = new Vector2D(robot.getCoords());
		Vector2D dir =  Vector2D.subtract(endPt, startPt);
		Vector2D ray = raytraceVector(ws, startPt, dir, am_i_blue, include_ball_as_obstacle);
		return ray.getLength();
	}

	/**
	 * Returns the distance to the collision in a given direction
	 * @param ws
	 * @param robot
	 * @param startPt
	 * @param endPt
	 * @return
	 */
	public static double reachabilityRight2(WorldState ws, Vector2D endPt, boolean am_i_blue, boolean include_ball_as_obstacle) {
		Robot robot = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot(); 
		Vector2D startPt = new Vector2D(robot.getCoords());
		Vector2D dir =  Vector2D.subtract(endPt, startPt);
		double angle = (-Vector2D.getDirection(dir)+90)*Math.PI/180d;
		final double length = Robot.LENGTH_CM/2;
		double cos = Math.cos(angle)*length;
		double sin = Math.sin(angle)*length;
		Vector2D left = new Vector2D(cos, sin);

		Vector2D ray_right = raytraceVector(ws, Vector2D.add(startPt, left), dir, am_i_blue, include_ball_as_obstacle);
		return ray_right.getLength();
	}

	/**
	 * Returns the distance to the collision in a given direction
	 * @param ws
	 * @param robot
	 * @param startPt
	 * @param endPt
	 * @return
	 */
	public static double reachabilityLeft2(WorldState ws, Vector2D endPt, boolean am_i_blue, boolean include_ball_as_obstacle) {
		Robot robot = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot(); 
		Vector2D startPt = new Vector2D(robot.getCoords());
		Vector2D dir =  Vector2D.subtract(endPt, startPt);
		double angle = (-Vector2D.getDirection(dir)+90)*Math.PI/180d;
		final double length = Robot.LENGTH_CM/2;
		double cos = Math.cos(angle)*length;
		double sin = Math.sin(angle)*length;
		Vector2D right = new Vector2D(-cos, -sin);

		Vector2D ray_left = raytraceVector(ws, Vector2D.add(startPt, right), dir, am_i_blue, include_ball_as_obstacle);
		return ray_left.getLength();
	}


	/**
	 * Whether you could reach this point if you turn into its direction and go into straight line.
	 * @param ws
	 * @param robot
	 * @param startPt
	 * @param endPt
	 * @return
	 */
	public static boolean reachability(WorldState ws, Vector2D endPt, boolean am_i_blue, boolean include_ball_as_obstacle) {
		if (!visibility(ws, endPt, am_i_blue, include_ball_as_obstacle))
			return false;
		Robot robot = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot(); 
		Vector2D startPt = new Vector2D(robot.getCoords());
		Vector2D dir =  Vector2D.subtract(endPt, startPt);
		double dir_l = dir.getLength();
		double angle = (-Vector2D.getDirection(dir)+90)*Math.PI/180d;
		final double length = Robot.LENGTH_CM/2;
		double cos = Math.cos(angle)*length;
		double sin = Math.sin(angle)*length;
		Vector2D left = new Vector2D(cos, sin);
		Vector2D right = new Vector2D(-cos, -sin);

		Vector2D ray_left = raytraceVector(ws, Vector2D.add(startPt, left), dir, am_i_blue, include_ball_as_obstacle);
		if (ray_left.getLength() < dir_l)
			return false;
		Vector2D ray_right = raytraceVector(ws, Vector2D.add(startPt, right), dir, am_i_blue, include_ball_as_obstacle);
		return ray_right.getLength() >= dir_l;
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
			return Vector2D.change_length(direction, length);
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
			final Vector2D distV = raytraceVector(ws, me, zero, new Vector2D(-Math.cos(ang_rad), Math.sin(ang_rad)), am_i_blue, include_ball_as_obstacle);
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


}
