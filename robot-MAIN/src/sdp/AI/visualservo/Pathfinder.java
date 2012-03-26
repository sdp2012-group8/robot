package sdp.AI.visualservo;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import sdp.AI.AIWorldState;
import sdp.common.geometry.Circle;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.common.world.WorldState;


/**
 * The pathfinder. This class provides facilities to compute paths to get
 * from one point to another one on the pitch.
 */
public class Pathfinder {
	
	/** The amount, by which the collision points are pushed from obstacles. */
	private static double COLLISION_ADJUSTMENT = 10.0;
	/** Largest number of waypoints a path can consist of. */
	private static int MAX_WAYPOINT_COUNT = 10;
	
	
	/**
	 * Create a new pathfinder instance.
	 */
	public Pathfinder() { }
	
	
	/**
	 * Get a path for a robot from one point to another one.
	 * 
	 * @param worldState Current world state.
	 * @param start Starting point.
	 * @param startAngle Robot's orientation at the start point.
	 * @param dest Destination point.
	 * @param obstacleFlag A bitfield that defines which objects should be
	 * 		considered as obstacles.
	 * @return The path to the destination as a sequence of waypoints.
	 */
	private ArrayList<Waypoint> getPath(AIWorldState worldState, Point2D.Double start,
			double startAngle, Point2D.Double dest, int obstacleFlag, int depth) {
		Vector2D startVec = new Vector2D(start);
		Vector2D destVec = new Vector2D(dest);
		
		Vector2D destDir = Vector2D.subtract(destVec, startVec);
		
		if (!WorldState.isPointInPitch(start) || !WorldState.isPointInPitch(dest)) {
			return null;
		}
		if (depth >= MAX_WAYPOINT_COUNT) {
			return null;
		}
		
		Vector2D startVecAdj = startVec;
		ArrayList<Circle> obstacles = WorldState.getObstacleCircles(worldState, obstacleFlag);
		
		for (Circle curObstacle : obstacles) {
			if (curObstacle.containsPoint(startVec)) {
				startVecAdj = new Vector2D(GeomUtils.changePointDistanceToCircle(curObstacle,
						startVec, curObstacle.getRadius() + COLLISION_ADJUSTMENT));
			}
		}
		
		if (WorldState.isDirectPathClear(worldState, startVecAdj, destVec, obstacleFlag)) {
			Vector2D destDirLocal = Vector2D.rotateVector(destDir, -startAngle);
			
			ArrayList<Waypoint> path = new ArrayList<Waypoint>();
			path.add(new Waypoint(destDirLocal, destDir.getLength(), true));
			return path;
		}
		
		double minPathCost = Double.MAX_VALUE;
		ArrayList<Waypoint> bestPath = null;
		
		for (Circle curObstacle : obstacles) {
			if (true) {
				Point2D.Double obsPoints[] = GeomUtils.circleTangentPoints(curObstacle, startVecAdj);
				
				if (obsPoints != null) {
					for (Point2D.Double pt : obsPoints) {
						pt = GeomUtils.changePointDistanceToCircle(curObstacle, pt,
								curObstacle.getRadius() + COLLISION_ADJUSTMENT);
						Vector2D ptDir = Vector2D.subtract(new Vector2D(pt), startVecAdj);
						
						if (!WorldState.isDirectPathClear(worldState, startVecAdj, new Vector2D(pt), obstacleFlag)) {
							continue;
						}
						
						ArrayList<Waypoint> curPath = getPath(worldState, pt, ptDir.getDirection(),
								dest, obstacleFlag, depth + 1);
						
						if (curPath != null) {
							double curCost = curPath.get(0).getCostToDest();
							if (curCost < minPathCost) {
								minPathCost = curCost;
								bestPath = curPath;
								
								Point2D.Double ptLocal = GeomUtils.getLocalPoint(startVec,
										Vector2D.getDirectionUnitVector(startAngle), pt);
								bestPath.add(0, new Waypoint(new Vector2D(ptLocal),
										curCost + ptDir.getLength(), false));
							}
						}
					}
				}
			}
		}
		
		return bestPath;
	}
	
	
	/**
	 * Get a path for our robot.
	 * 
	 * @param worldState Current world state.
	 * @param dest Destination point.
	 * @param ballIsObstacle Whether the ball should be considered an obstacle.
	 * @return The path to the destination as a sequence of waypoints.
	 */
	public ArrayList<Waypoint> getPathForOwnRobot(AIWorldState worldState,
			Point2D.Double dest, boolean ballIsObstacle) {
		int obstacles = WorldState.makeObstacleFlagsForOpponent(ballIsObstacle,
				worldState.isOwnTeamBlue());
		return getPath(worldState, worldState.getOwnRobot().getCoords(),
				worldState.getOwnRobot().getAngle(), dest, obstacles, 0);
	}

}
