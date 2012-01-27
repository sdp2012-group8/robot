package sdp.common;

import java.awt.Point;


/**
 * This class represents a robot on the field.
 * 
 * @author Gediminas Liktaras
 */
public final class Robot {
	
	/** Length of the robot's top plate. */
	public static final int LENGTH = 70;
	/** Width of the robot's top plate. */
	public static final int WIDTH = 50;
	

	/** Coordinates of the robot's center on the field. */
	private Point coords;
	/** The angle the robot is facing, in radians. */
	private double angle;
	
	/** Coordinates of the robot's front-left corner. */
	private Point frontLeftPoint;
	/** Coordinates of the robot's front-right corner. */
	private Point frontRightPoint;
	/** Coordinates of the robot's back-left corner. */
	private Point backLeftPoint;
	/** Coordinates of the robot's back-right corner. */
	private Point backRightPoint;

	
	/**
	 * The main constructor.
	 * 
	 * @param coords The robot's coordinates.
	 * @param angle The angle the robot is facing, in radians.
	 */
	public Robot(Point coords, double angle) {
		this.coords = coords;
		this.angle = angle;
		
		frontLeftPoint = Utilities.rotatePoint(new Point(0, 0), new Point(LENGTH / 2, WIDTH / 2), angle);
		frontLeftPoint.translate(coords.x, coords.y);
		
		frontRightPoint = Utilities.rotatePoint(new Point(0, 0), new Point(LENGTH / 2, -WIDTH / 2), angle);
		frontRightPoint.translate(coords.x, coords.y);
		
		backLeftPoint = Utilities.rotatePoint(new Point(0, 0), new Point(-LENGTH / 2, WIDTH / 2), angle);
		backLeftPoint.translate(coords.x, coords.y);
		
		backRightPoint = Utilities.rotatePoint(new Point(0, 0), new Point(-LENGTH / 2, -WIDTH / 2), angle);
		backRightPoint.translate(coords.x, coords.y);
	}

	
	/**
	 * Get coordinates of the robot's center.
	 * 
	 * @return Coordinates of the robot's center.
	 */
	public final Point getCoords() {
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
	public final Point getFrontLeft() {
		return frontLeftPoint;
	}

	/**
	 * Get coordinates of the front-right corner of the robot's rectangle.
	 * 
	 * @return Coordinates of the front-right corner of the robot's rectangle.
	 */
	public final Point getFrontRight() {
		return frontRightPoint;
	}

	/**
	 * Get coordinates of the back-left corner of the robot's rectangle.
	 * 
	 * @return Coordinates of the back-left corner of the robot's rectangle.
	 */
	public final Point getBackLeft() {
		return backLeftPoint;
	}

	/**
	 * Get coordinates of the back-right corner of the robot's rectangle.
	 * 
	 * @return Coordinates of the back-right corner of the robot's rectangle.
	 */
	public final Point getBackRight() {
		return backRightPoint;
	}

}