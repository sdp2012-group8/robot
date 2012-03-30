package sdp.AI.pathfinding;

import sdp.common.geometry.Vector2D;


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
	
	/** Path's remaining cost. */
	private double costToDest;
	/** Whether the endpoint at the end of a path. */
	private boolean isEndpoint;
	
	
	/**
	 * Create a new waypoint.
	 * 
	 * This function assumes that the robot is placed at coordinates
	 * (0, 0) and is facing direction of 0 degrees.
	 * 
	 * @param target Target point in robot-relative coordinates.
	 * @param isEndpoint Whether the target is at the end of a path.
	 */
	@Deprecated
	public Waypoint(Vector2D target, boolean isEndpoint) {
		this(target, 0.0, isEndpoint);
	}
	
	/**
	 * Create a new waypoint.
	 * 
	 * This function assumes that the robot is placed at coordinates
	 * (0, 0) and is facing direction of 0 degrees.
	 * 
	 * @param target Target point in robot-relative coordinates.
	 * @param costToDest Cost of the path from here to the destination.
	 * @param isEndpoint Whether the target is at the end of a path.
	 */
	public Waypoint(Vector2D target, double costToDest, boolean isEndpoint) {
		this.targetPoint = target;
		this.costToDest = costToDest;
		this.isEndpoint = isEndpoint;
		
		targetDist = targetPoint.getLength();
		targetTurnAngle = targetPoint.getDirection();
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
	 * Get the remaining cost of the path.
	 * 
	 * @return Remaining path cost.
	 */
	public final double getCostToDest() {
		return costToDest;
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
	

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("<%s %.4f %.4f %.4f>", targetPoint.toString(),
				targetDist, targetTurnAngle, costToDest);
	}
}
