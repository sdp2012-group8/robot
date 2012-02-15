package sdp.common;

import java.awt.geom.Point2D;


/**
 * This class represents a robot on the field.
 * 
 * @author Gediminas Liktaras
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

	
	/**
	 * The main constructor.
	 * 
	 * @param coords The robot's coordinates. In 0..1, use {@link #setCoords(boolean)} for cm.
	 * @param angle The angle the robot is facing, in degrees.
	 */
	public Robot(Point2D.Double coords, double angle) {
		setCoords(coords, angle, false);
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
		double angle_rad = Math.toDegrees(angle);
		this.coords = coords;
		this.angle = angle;
		
		double length = cm ? LENGTH_CM : LENGTH;
		double width = cm ? WIDTH_CM : WIDTH;
		
		frontLeftPoint = Utilities.rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(length / 2, width / 2), angle_rad);
		Utilities.translatePoint(frontLeftPoint, coords);
		
		frontRightPoint = Utilities.rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(length / 2, -width / 2), angle_rad);
		Utilities.translatePoint(frontRightPoint, coords);
		
		backLeftPoint = Utilities.rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(-length / 2, width / 2), angle_rad);
		Utilities.translatePoint(backLeftPoint, coords);
		
		backRightPoint = Utilities.rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(-length / 2, -width / 2), angle_rad);
		Utilities.translatePoint(backRightPoint, coords);
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
}