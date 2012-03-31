package sdp.AI.pathfinding;

import sdp.common.geometry.Vector2D;


/**
 * Container for a path waypoint.
 * 
 * @author Gediminas Liktaras
 */
public final class Waypoint {
	
	/** Starting position. */
	private Vector2D originPos;
	/** Starting direction. */
	private double originDir;
	/** Point to go towards in global coordinates. */
	private Vector2D target;

	/** Point to go towards in robot-relative coordinates. */
	private Vector2D targetLocal;
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
	 * @param originPos Starting robot coordinates in global coordinates.
	 * @param originDir Starting robot direction in degrees.
	 * @param target Target point in global coordinates.
	 * @param costToDest Cost of the path from here to the final destination.
	 * @param isEndpoint Whether the target is at the end of the path.
	 */
	public Waypoint(Vector2D originPos, double originDir, Vector2D target,
			double costToDest, boolean isEndpoint) {
		this.originPos = originPos;
		this.originDir = originDir;
		this.target = target;
		this.costToDest = costToDest;
		this.isEndpoint = isEndpoint;
		
		targetLocal = Vector2D.subtract(target, originPos);
		targetLocal = Vector2D.rotateVector(targetLocal, -originDir);
		
		targetDist = targetLocal.getLength();
		targetTurnAngle = targetLocal.getDirection();
	}
	
	
	/**
	 * Get the starting position.
	 * 
	 * @return Starting position.
	 */
	public final Vector2D getOriginPos() {
		return originPos;
	}

	/**
	 * Get the starting direction.
	 * 
	 * @return Starting direction.
	 */
	public final double getOriginDir() {
		return originDir;
	}
	

	/**
	 * Get the destination in global coordinates.
	 * 
	 * @return Destination in global coordinates.
	 */
	public final Vector2D getTarget() {
		return target;
	}

	/**
	 * Get the destination is robot-relative coordinates.
	 * 
	 * @return Destination in local coordinates.
	 */
	public final Vector2D getTargetLocal() {
		return targetLocal;
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
		return String.format("<%s %.2f %s %s %.4f %.4f %.4f>", originPos.toString(),
				originDir, target.toString(), targetLocal.toString(), targetDist,
				targetTurnAngle, costToDest);
	}
}
