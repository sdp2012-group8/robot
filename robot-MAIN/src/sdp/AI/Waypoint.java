package sdp.AI;

import sdp.common.Robot;
import sdp.common.Utilities;
import sdp.common.Vector2D;


/**
 * Container for a path waypoint.
 * 
 * @author Gediminas Liktaras
 */
public final class Waypoint {

	/** Point to get to in robot-relative coordinates. */
	private Vector2D targetPoint;
	/** Distance to the target point. */
	private double targetDist;
	/** The angle by which to turn to face the target. */
	private double targetTurnAngle;
	
	/** Whether the endpoint at the end of a path. */
	private boolean isEndpoint;
	
	
	/**
	 * Create a new waypoint for a robot.
	 * 
	 * @param robot Robot's current location.
	 * @param target Target point.
	 * @param isEndpoint Whether the target is at the end of a path.
	 */
	public Waypoint(Robot robot, Vector2D target, boolean isEndpoint) {
		Vector2D robotCoords = new Vector2D(robot.getCoords());
		
		this.targetPoint = Vector2D.subtract(target, robotCoords);
		this.isEndpoint = isEndpoint;
		
		targetDist = targetPoint.getLength();
		targetTurnAngle = Utilities.getTurningAngle(robot, target);
	}
	
	
	/**
	 * Get the waypoint coordinates in robot-relative coordinate space.
	 * 
	 * @return Next waypoint coordinates.
	 */
	public final Vector2D getLocalCoords() {
		return targetPoint;
	}

	/**
	 * Get the distance to the waypoint.
	 * 
	 * @return Distance to the waypoint.
	 */
	public final double getDistance() {
		return targetDist;
	}

	/**
	 * Get the angle the robot has to turn in order to face the waypoint.
	 * 
	 * @return The turning angle.
	 */
	public final double getTurningAngle() {
		return targetTurnAngle;
	}

	/**
	 * Get whether the current waypoint lies at the end of the path.
	 * 
	 * @return If this is the final point.
	 */
	public final boolean isEndpoint() {
		return isEndpoint;
	}
}
