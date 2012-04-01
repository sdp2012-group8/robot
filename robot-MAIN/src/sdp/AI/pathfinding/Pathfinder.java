package sdp.AI.pathfinding;

import java.awt.geom.Point2D;

import sdp.AI.AIWorldState;


/**
 * Interface for all pathfinder implementations.
 */
public interface Pathfinder {
	
	/**
	 * Find the next point for our robot to go to.
	 * 
	 * @param worldState Current world state.
	 * @param dest Destination point.
	 * @param ballIsObstacle Whether the ball should be considered an obstacle.
	 * @return The next waypoint on the path.
	 */
	public abstract Waypoint getNextWaypoint(AIWorldState worldState,
			Point2D.Double dest, boolean ballIsObstacle);
}