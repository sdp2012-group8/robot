package sdp.common;

import java.awt.geom.Point2D;


/**
 * This class represents a robot on the field.
 * 
 * @author Gediminas Liktaras
 */
public final class Robot {
	
	/** Length of the robot's top plate. */
	public static final double LENGTH = 70.0;
	/** Width of the robot's top plate. */
	public static final double WIDTH = 50.0;
	

	/** Coordinates of the robot's center on the field. */
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
	 * @param coords The robot's coordinates.
	 * @param angle The angle the robot is facing, in radians.
	 */
	public Robot(Point2D.Double coords, double angle) {
		this.coords = coords;
		this.angle = angle;
		
		frontLeftPoint = Utilities.rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(LENGTH / 2, WIDTH / 2), angle);
		Utilities.translatePoint(frontLeftPoint, coords);
		
		frontRightPoint = Utilities.rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(LENGTH / 2, -WIDTH / 2), angle);
		Utilities.translatePoint(frontRightPoint, coords);
		
		backLeftPoint = Utilities.rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(-LENGTH / 2, WIDTH / 2), angle);
		Utilities.translatePoint(backLeftPoint, coords);
		
		backRightPoint = Utilities.rotatePoint(new Point2D.Double(0, 0), new Point2D.Double(-LENGTH / 2, -WIDTH / 2), angle);
		Utilities.translatePoint(backRightPoint, coords);
	}

	
	/**
	 * Get coordinates of the robot's center.
	 * 
	 * @return Coordinates of the robot's center.
	 */
	public final Point2D.Double getCoords() {
		return coords;
	}

	/**
	 * Get the angle the robot is facing, in radians.
	 * 
	 * @return The angle the robot is facing, in radians.
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