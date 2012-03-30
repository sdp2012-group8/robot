package sdp.AI.pathfinding;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;

import sdp.AI.AIWorldState;
import sdp.common.geometry.Circle;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.common.world.WorldState;


/**
 * The full pathfinder. This class provides facilities to compute paths to
 * get from one point to another one on the pitch. It does so by considering
 * full paths towards the destination and picking the shortest one.
 */
public class FullPathfinder implements Pathfinder {
	
	/** Radius of checked circles. */
	private static double CHECKED_CIRCLE_RADIUS = 2.5;
	/** The amount, by which the collision points are pushed from obstacles. */
	private static double COLLISION_ADJUSTMENT = 10.0;
	/** Largest number of waypoints a path can consist of. */
	private static int MAX_WAYPOINT_COUNT = 10;
	
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
	
	private ArrayList<Waypoint> checkForSolvedInstance(Vector2D checkPt, Vector2D point, double dir) {
		for (PartialPath p : partialAnswers) {
			if (p.getPathEnd().containsPoint(checkPt)) {
				ArrayList<Waypoint> bestPath = p.getWaypoints();
				if (bestPath != null) {
					Waypoint newSegment = new Waypoint(point, dir,
							bestPath.get(0).getTarget(), bestPath.get(0).getCostToDest(), true);
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
		
		if (!WorldState.isPointInPitch(startVecAdj) || !WorldState.isPointInPitch(dest)) {
			return null;
		}
		if (depth >= MAX_WAYPOINT_COUNT) {
			return null;
		}
		
		if (WorldState.isDirectPathClear(worldState, startVecAdj, destVec, obstacleFlag)) {
			ArrayList<Waypoint> path = new ArrayList<Waypoint>();
			path.add(new Waypoint(startVec, startAngle, destVec, destDir.getLength(), true));
			return path;
		}
		
		ArrayList<Waypoint> bestPath = checkForSolvedInstance(startVecAdj,
				startVec, startAngle);
		if (bestPath != null) {
			partialAnswers.add(new PartialPath(new Circle(startVec, COLLISION_ADJUSTMENT), bestPath));
			return bestPath;
		}
		
		checkedPoints.push(new Circle(startVecAdj, CHECKED_CIRCLE_RADIUS));

		double minPathCost = Double.MAX_VALUE;
				
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
				
				for (Circle c : checkedPoints) {
					if (c.containsPoint(pt)) {
						continue obstaclePointLoop;
					}
				}
				if (!WorldState.isDirectPathClear(worldState, startVecAdj,
						new Vector2D(pt), obstacleFlag)) {
					continue;
				}
				
				ArrayList<Waypoint> curPath = getPath(worldState, pt, ptDir.getDirection(),
						dest, obstacleFlag, depth + 1);
				
				if (curPath != null) {
					double curCost = curPath.get(0).getCostToDest() + ptDir.getLength();
					if (curCost < minPathCost) {
						minPathCost = curCost;
						bestPath = curPath;
						
						Point2D.Double ptLocal = GeomUtils.getLocalPoint(startVec,
								Vector2D.getDirectionUnitVector(startAngle), pt);
						bestPath.add(0, new Waypoint(new Vector2D(ptLocal),
								curCost, false));
						
						bestPath.add(new Waypoint(startVec, startAngle, new Vector2D(pt),
								curCost, false));
					}
				}
			}
		}
		
		//checkedPoints.pop();		
		partialAnswers.add(new PartialPath(new Circle(startVecAdj,
				CHECKED_CIRCLE_RADIUS), bestPath));
		
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
	 * @see sdp.AI.pathfinding.Pathfinder#getWaypointForOurRobot(sdp.AI.AIWorldState, java.awt.geom.Point2D.Double, boolean)
	 */
	@Override
	public Waypoint getWaypointForOurRobot(AIWorldState worldState,
			java.awt.geom.Point2D.Double dest, boolean ballIsObstacle) {
		ArrayList<Waypoint> path = getPathForOwnRobot(worldState, dest, ballIsObstacle);
		
		if (path == null) {
			return fallback.getWaypointForOurRobot(worldState, dest, ballIsObstacle);
		} else {
			return path.get(0);
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
