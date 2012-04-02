package sdp.AI.pathfinding;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;

import sdp.AI.AIWorldState;
import sdp.common.Painter;
import sdp.common.geometry.Circle;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;


/**
 * The full pathfinder. This class provides facilities to compute paths to
 * get from one point to another one on the pitch. It does so by considering
 * full paths towards the destination and picking the shortest one.
 */
public class FullPathfinder implements Pathfinder {
	
	/** Whether to ignore walls in pathfinding. */
	private static boolean IGNORE_WALLS = false;
	/** Whether to use memoization optimisation. */
	private static boolean USE_MEMOIZATION = false;
	
	/** Radius of checked circles. */
	private static double CHECKED_CIRCLE_RADIUS = 0.0;
	/** The amount, by which the collision points are pushed from obstacles. */
	private static double COLLISION_ADJUSTMENT = 5.0;
	/** Largest number of waypoints a path can consist of. */
	private static int MAX_WAYPOINT_COUNT = 4;
	
	
	/** A list of points that have been explored in a search. */
	private LinkedList<Circle> checkedPoints = new LinkedList<Circle>();
	/** Pathfinder's partial answers. */
	private ArrayList<PartialPath> partialAnswers = new ArrayList<PartialPath>();
	
	/** A fallback pathfinder. */
	private HeuristicPathfinder fallback = new HeuristicPathfinder();
	
	
	/**
	 * Create a new full pathfinder.
	 */
	public FullPathfinder() { }
	
	
	/**
	 * Move the given point out of all obstacles.
	 * 
	 * @param obstacles A list of obstacles.
	 * @param point Point of interest.
	 * @return The given point, moved out of obstacles.
	 */
	private Vector2D movePointOutOfObstacle(ArrayList<Circle> obstacles,
			Vector2D point) {
		Vector2D newPoint = new Vector2D(point.x, point.y);
		
		boolean posChanged = true;
		while (posChanged) {
			posChanged = false;
			
			for (Circle curObstacle : obstacles) {
				if (curObstacle.containsPoint(newPoint)) {
					newPoint = new Vector2D(GeomUtils.changePointDistanceToCircle(curObstacle,
							newPoint, curObstacle.getRadius() + COLLISION_ADJUSTMENT));
					posChanged = true;
				}
			}
		}
		
		return newPoint;
	}
	
	/**
	 * Check if there is a solved pathfinding problem from the given point and
	 * return the stored solution.
	 * 
	 * @param checkPt Point to use for a solution check.
	 * @param point Current robot position.
	 * @param dir Current robot direction.
	 * @return Cached solution if one exists and null if it does not.
	 */
	private ArrayList<Waypoint> checkForSolvedInstance(Vector2D checkPt, Vector2D point, double dir) {
		if (!USE_MEMOIZATION) {
			return null;
		}
		
		for (PartialPath p : partialAnswers) {
			if (p.getPathEnd().containsPoint(checkPt)) {
				ArrayList<Waypoint> bestPath = p.getWaypoints();
				if (bestPath != null) {
					double newDist = bestPath.get(0).getCostToDest();
					newDist += GeomUtils.pointDistance(point, bestPath.get(0).getTarget());
					if (bestPath.size() > 1) {
						newDist -= bestPath.get(1).getCostToDest();
					}
					
					Waypoint newSegment = new Waypoint(point, dir,
							bestPath.get(0).getTarget(), newDist, true);
					bestPath.set(0, newSegment);
				}
				return bestPath;
			}
		}
		
		return null;
	}
	
	
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
				
		ArrayList<Circle> obstacles = WorldState.getObstacleCircles(worldState, obstacleFlag);
		Vector2D startVecAdj = movePointOutOfObstacle(obstacles, startVec);
		
		// Check for failure conditions.
		if (!WorldState.isPointInPitch(startVecAdj) || !WorldState.isPointInPitch(dest)) {
			return null;
		}
		if (depth >= MAX_WAYPOINT_COUNT) {
			return null;
		}
		
		// Check for a direct path solution.
		if (WorldState.isDirectPathClear(worldState, startVecAdj, destVec, obstacleFlag)) {
			ArrayList<Waypoint> path = new ArrayList<Waypoint>();
			path.add(new Waypoint(startVec, startAngle, destVec, destDir.getLength(), true));
			return path;
		}
		
		// Check for a cached solution.
		ArrayList<Waypoint> bestPath = checkForSolvedInstance(startVecAdj,
				startVec, startAngle);
		if (bestPath != null) {
			return bestPath;
		}
		
		// Start recursive descent.
		checkedPoints.push(new Circle(startVecAdj, CHECKED_CIRCLE_RADIUS));

		double minPathCost = Double.MAX_VALUE;
		
		int directCheckFlags = obstacleFlag;
		if (IGNORE_WALLS) {
			directCheckFlags &= ~(WorldState.WALL_IS_OBSTACLE_FLAG);
		}
				
		for (Circle curObstacle : obstacles) {
			Point2D.Double obsPoints[] = GeomUtils.circleTangentPoints(curObstacle, startVecAdj);
			if (obsPoints == null) {
				continue;
			}
			
			obstaclePointLoop:
			for (Point2D.Double pt : obsPoints) {
				pt = GeomUtils.changePointDistanceToCircle(curObstacle, pt,
						curObstacle.getRadius() + COLLISION_ADJUSTMENT);
				Vector2D ptDir = Vector2D.subtract(new Vector2D(pt), startVecAdj);
				
				if (!WorldState.isPointInPaddedPitch(pt, Robot.LENGTH_CM / 2)) {
					continue;
				}
				for (Circle c : checkedPoints) {
					if (c.containsPoint(pt)) {
						continue obstaclePointLoop;
					}
				}
				if (!WorldState.isDirectPathClear(worldState, startVecAdj,
						new Vector2D(pt), directCheckFlags)) {
					continue;
				}
				
				ArrayList<Waypoint> curPath = getPath(worldState, pt, ptDir.getDirection(),
						dest, obstacleFlag, depth + 1);
				
				if (curPath != null) {
					double curCost = curPath.get(0).getCostToDest() + ptDir.getLength();
					if (curCost < minPathCost) {
						minPathCost = curCost;
						bestPath = curPath;
						
						bestPath.add(0, new Waypoint(startVec, startAngle,
								new Vector2D(pt), curCost, false));
					}
				}
			}
		}
		
		if (!checkedPoints.isEmpty()) {
			// Should never be empty here, but we did get an error once. Could
			// have been a hotplugging issue.
			checkedPoints.pop();
		}
		
		if (USE_MEMOIZATION) {
			partialAnswers.add(new PartialPath(new Circle(startVecAdj,
					CHECKED_CIRCLE_RADIUS), bestPath));
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
		checkedPoints.clear();
		partialAnswers.clear();
		
		int obstacles = WorldState.makeObstacleFlagsForOpponent(ballIsObstacle,
				worldState.isOwnTeamBlue());
		return getPath(worldState, worldState.getOwnRobot().getCoords(),
				worldState.getOwnRobot().getAngle(), dest, obstacles, 0);
	}


	/**
	 * @see sdp.AI.pathfinding.Pathfinder#getNextWaypoint(sdp.AI.AIWorldState, java.awt.geom.Point2D.Double, boolean)
	 */
	@Override
	public ArrayList<Waypoint> getPath(AIWorldState worldState,
			java.awt.geom.Point2D.Double dest, boolean ballIsObstacle) {
		ArrayList<Waypoint> path = getPathForOwnRobot(worldState, dest, ballIsObstacle);
		Painter.fullPath = path;
		
		if (path == null) {
			return fallback.getPath(worldState, dest, ballIsObstacle);
		} else {
			return path;
		}
	}

}


/**
 * A partial path container.
 */
class PartialPath {
	
	/** The point where the path ends. */
	private Circle pathEnd;
	/** Path's waypoints. */
	private ArrayList<Waypoint> waypoints;
	
	
	/**
	 * Create a new partial path.
	 * 
	 * @param pathEnd Endpoint of the path.
	 * @param waypoints Waypoints of the path.
	 */
	public PartialPath(Circle pathEnd, ArrayList<Waypoint> waypoints) {
		this.pathEnd = pathEnd;
		this.waypoints = waypoints;
	}
	
	
	/**
	 * Get the end of the path.
	 * 
	 * @return Endpoint of the path.
	 */
	public Circle getPathEnd() {
		return pathEnd;
	}
	
	/**
	 * Get the waypoints of the path.
	 * 
	 * Note that this method returns a copy of the waypoints.
	 * 
	 * @return Waypoints of the path.
	 */
	public ArrayList<Waypoint> getWaypoints() {
		if (waypoints == null) {
			return null;
		} else {
			return new ArrayList<Waypoint>(waypoints);
		}
	}
}
