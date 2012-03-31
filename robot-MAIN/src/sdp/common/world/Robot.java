package sdp.common.world;

import java.awt.geom.Point2D;

import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;


/**
 * This class represents a robot on the field.
 */
public final class Robot {
	
	/** Length of the robot's top plate in centimeters. */
	public static final double LENGTH_CM = 20;	
	/** Length of the robot's top plate in normalised coordinates. */
	public static final double LENGTH = LENGTH_CM / 244d;
	
	/** Width of the robot's top plate in centimeters. */
	public static final double WIDTH_CM = 18;	
	/** Width of the robot's top plate in normalised coordinates. */
	public static final double WIDTH = WIDTH_CM / 244d;
	
	/** Default robot acceleration speed in cm/s^2. */
	public static final double ACCELERATION_SPEED = 69.81317;
	/** Maximum robot driving speed in cm/s. */
	public static final int MAX_DRIVING_SPEED = 53;
	/** Maximum robot turning speed in zig/ptk. */
	public static final int MAX_TURNING_SPEED = 127;


	/** Coordinates of the robot's centre on the field. */
	private Point2D.Double coords;
	/** The angle the robot is facing, in radians. */
	private double angle;
	
	/** Coordinates of the robot's front-left corner. */
	private Point2D.Double frontLeftPoint;
	/** Coordinates of the robot's front-right corner. */
	private Point2D.Double frontRightPoint;
	/** Coordinates of the robot's back-left corner. */
	private Point2D.Double backLeftPoint;
	/** Coordinates of the robot's back-right corner. */
	private Point2D.Double backRightPoint;
	/** Coordinates of the robot's front-center. */
	private Point2D.Double frontCenterPoint;
	/** Coordinates of the robot's back-center. */
	private Point2D.Double backCenterPoint;
	
	
	/**
	 * Create a new robot.
	 * 
	 * @param coords The robot's coordinates in normal coordinates.
	 * @param angle The angle the robot is facing, in degrees.
	 */
	public Robot(Point2D.Double coords, double angle) {
		setCoords(coords, angle, false);
	}
	
	/**
	 * Create a new robot.
	 * 
	 * @param coords The robot's coordinates, in normal coordinates or centimeters.
	 * @param angle The angle the robot is facing, in degrees.
	 * @param inCm Whether coords argument is in centimeters.
	 */
	public Robot(Point2D.Double coords, double angle, boolean inCm) {
		setCoords(coords, angle, inCm);
	}
	
	
	/**
	 * Set angle
	 * @param angle in degrees
	 */
	public final void setCoords(double angle) {
		setCoords(coords, angle, false);
	}
	
	/**
	 * Set coordinates only
	 * @param coords in 0..1, use {@link #setCoords(boolean)} for cm.
	 */
	public final void setCoords(Point2D.Double coords) {
		setCoords(coords, angle, false);
	}
		
	/**
	 * Sets the coordinates of the robot
	 * @param coords
	 * @param angle in degrees
	 * @param cm Are coordinates in cm?
	 */
	public final void setCoords(Point2D.Double coords, double angle, boolean cm) {
		this.coords = coords;
		this.angle = GeomUtils.normaliseAngle(angle);
		
		double length = (cm ? LENGTH_CM : LENGTH);
		double width = (cm ? WIDTH_CM : WIDTH);
		
		Point2D.Double corners[] = GeomUtils.positionRectangle(length,
				width, coords, angle);
		frontLeftPoint = corners[0];
		frontRightPoint = corners[1];
		backRightPoint = corners[2];
		backLeftPoint = corners[3];
		
		frontCenterPoint = GeomUtils.rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(length / 2, 0), angle);
		GeomUtils.translatePoint(frontCenterPoint, coords);
		
		backCenterPoint = GeomUtils.rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(-length / 2, 0), angle);
		GeomUtils.translatePoint(backCenterPoint, coords);
	}

	
	/**
	 * Converts coordinates that the robot was initialised with in cm or in 0..1
	 * @param cm
	 */
	public final void setCoords(boolean cm) {
		setCoords(coords, angle, cm);
	}

	
	/**
	 * Get coordinates of the robot's centre.
	 * 
	 * @return Coordinates of the robot's centre.
	 */
	public final Point2D.Double getCoords() {
		return coords;
	}

	/**
	 * Get the angle the robot is facing, in degrees.
	 * 
	 * @return The angle the robot is facing, in degrees.
	 */
	public final double getAngle() {
		return angle;
	}
	
	/**
	 * Get the direction the robot is facing as a vector.
	 * 
	 * @return The direction of the robot.
	 */
	public final Vector2D getDirection() {
		return Vector2D.getDirectionUnitVector(angle);
	}
	
	
	/**
	 * Get the top image of the robot. Coordinates will be expressed in
	 * centimetres.
	 * 
	 * @return The top image of the robot.
	 */
	public final Robot getTopImage(){
		return new Robot(new Point2D.Double(coords.x, -coords.y), -angle, true);
	}
	
	/**
	 * Returns the bottom image of the robot. Coordinates will be expressed
	 * in centimetres.
	 * 
	 * @return The bottom image of the robot.
	 */
	public final Robot getBottomImage(){
		return new Robot(new Point2D.Double(coords.x, 2 * WorldState.PITCH_HEIGHT_CM - coords.y), -angle, true);
	}

	
	/**
	 * Get coordinates of the front-left corner of the robot's rectangle.
	 * 
	 * @return Coordinates of the front-left corner of the robot's rectangle.
	 */
	public final Point2D.Double getFrontLeft() {
		return frontLeftPoint;
	}

	/**
	 * Get coordinates of the front-right corner of the robot's rectangle.
	 * 
	 * @return Coordinates of the front-right corner of the robot's rectangle.
	 */
	public final Point2D.Double getFrontRight() {
		return frontRightPoint;
	}

	/**
	 * Get coordinates of the back-left corner of the robot's rectangle.
	 * 
	 * @return Coordinates of the back-left corner of the robot's rectangle.
	 */
	public final Point2D.Double getBackLeft() {
		return backLeftPoint;
	}

	/**
	 * Get coordinates of the back-right corner of the robot's rectangle.
	 * 
	 * @return Coordinates of the back-right corner of the robot's rectangle.
	 */
	public final Point2D.Double getBackRight() {
		return backRightPoint;
	}
	
	/**
	 * Get coordinates of the front center of the robot.
	 * 
	 * @return Coordinates of the front center of robot.
	 */
	public final Point2D.Double getFrontCenter(){	
		return frontCenterPoint;
	}
	
	/**
	 * Get coordinates of the back center of the robot.
	 * 
	 * @return Coordinates of the back center of the robot.
	 */
	public final Point2D.Double getBackCenter(){
		return backCenterPoint;
	}
	

	/**
	 * Get how many degrees the robot will have to rotate to face the given
	 * point.
	 * 
	 * @param robot Robot in question.
	 * @param point Point the robot has to face.
	 * @return The angle by which the robot will have to turn to face the point,
	 * 		in degrees.
	 */
	public static double getTurningAngle(Robot robot, Point2D.Double point) {
		Vector2D pointLocal = getLocalVector(robot, new Vector2D(point));
		return GeomUtils.normaliseAngle(-robot.getAngle() + pointLocal.getDirection());
	}
	
	
	/**
	 * Convert a vector from the global coordinate system to a system local
	 * to the given robot.
	 * 
	 * @param robot Robot in question.
	 * @param globalVector Vector in global coordinates.
	 * @return Corresponding vector in local coordinates.
	 */
	public static Vector2D getLocalVector(Robot robot, Vector2D globalVector) {
		return new Vector2D(GeomUtils.getLocalPoint(robot.getCoords(), robot.getDirection(), globalVector));
	}

	/**
	 * Convert a vector from the coordinate system local to the given robot
	 * to the global one.
	 * 
	 * @param robot Robot in question.
	 * @param localVector Vector in local coordinates.
	 * @return Corresponding vector in global coordinates.
	 */
	public static Vector2D getGlobalVector(Robot robot, Vector2D localVector) {
		return new Vector2D(GeomUtils.getGlobalPoint(robot.getCoords(), robot.getDirection(), localVector));
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
	 * TODO: Clean up.
	 * 
	 * @param point Point of interest.
	 * @param robot Robot in question.
	 * @return Whether the point is around a robot.
	 */
	public static boolean isPointAroundRobot(Point2D.Double point, Robot robot) {
		Point2D.Double corners[] = GeomUtils.positionRectangle(2 * LENGTH_CM,
				WIDTH_CM + LENGTH_CM, robot.getCoords(), robot.getAngle());

		return GeomUtils.isPointInQuadrilateral(point, corners[0], corners[1],
				corners[2], corners[3]);
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
}