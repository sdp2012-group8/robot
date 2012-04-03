package sdp.AI.pathfinding;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import sdp.AI.AIWorldState;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;


/**
 * The heuristic pathfinder. This class provides facilities to compute paths
 * to get from one point to another one on the pitch. It does so by trying to
 * make a locally-best choice.
 */
public class HeuristicPathfinder implements Pathfinder {
	
	/** Number of direction sections, used in search. */
	private static final int COLLISION_SECTION_COUNT = 62;
	/** Size of an individual direction section, in angles. */
	private static final double SECTION_ANGLE = 360.0 / COLLISION_SECTION_COUNT;
	
	
	/**
	 * Create a new heuristic pathfinder.
	 */
	public HeuristicPathfinder() { }
	
	
	/**
	 * @see sdp.AI.pathfinding.Pathfinder#getNextWaypoint(sdp.AI.AIWorldState, sdp.common.geometry.Vector2D, boolean)
	 */
	@Override
	public ArrayList<Waypoint> getPath(AIWorldState aiWorldState,
			Point2D.Double target, boolean ballIsObstacle) {
		ArrayList<Waypoint> retValue = new ArrayList<Waypoint>();
		
		Vector2D targetVec = new Vector2D(target);		
		Vector2D targetVecLocal = Robot.getLocalVector(aiWorldState.getOwnRobot(), targetVec);
		int obstacleFlags = WorldState.makeObstacleFlagsForOpponent(ballIsObstacle, aiWorldState.isOwnTeamBlue());
	
		Vector2D ownCoords = new Vector2D(aiWorldState.getOwnRobot().getCoords());
		if (WorldState.isDirectPathClear(aiWorldState, ownCoords, targetVec, obstacleFlags)) {
			retValue.add(new Waypoint(ownCoords, aiWorldState.getOwnRobot().getAngle(),
					targetVec, targetVecLocal.getLength(), true));
			return retValue;
		}
	
		Vector2D destPoint = null;
		double destPointDist = targetVecLocal.getLength();
	
		double minAngle = Double.MAX_VALUE;
		int iterations = 0;
	
		while ((destPoint == null) && (iterations < 5) && (destPointDist > 0)) {
			for (int i = 0; i < COLLISION_SECTION_COUNT; i++) {
				double curAngle = -90 + i * SECTION_ANGLE + SECTION_ANGLE / 2;
				curAngle = GeomUtils.normaliseAngle(curAngle);
	
				Vector2D rayDir = Vector2D.rotateVector(new Vector2D(1, 0), curAngle);
				Vector2D rayEndLocal = Vector2D.multiply(rayDir, destPointDist);
				Vector2D rayEnd = Robot.getGlobalVector(aiWorldState.getOwnRobot(), rayEndLocal);
	
				if (WorldState.isDirectPathClear(aiWorldState, ownCoords, rayEnd, obstacleFlags)) {
					double angleDiff = GeomUtils.normaliseAngle(curAngle - targetVecLocal.getDirection());
					if (Math.abs(angleDiff) < Math.abs(minAngle)) {
						minAngle = angleDiff;
						destPoint = rayEnd;
					}
				}
			}
	
			++iterations;
			destPointDist -= Robot.LENGTH_CM;
		}
	
		if (destPoint == null) {
			destPoint = targetVec;
		}
	
		retValue.add(new Waypoint(new Vector2D(aiWorldState.getOwnRobot().getCoords()),
				aiWorldState.getOwnRobot().getAngle(), new Vector2D(destPoint),
				Vector2D.subtract(destPoint, ownCoords).getLength(), false));
		return retValue;
	}
}
