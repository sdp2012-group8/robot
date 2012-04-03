package sdp.AI.pathfinding;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import sdp.AI.AIWorldState;
import sdp.common.Painter;
import sdp.common.geometry.Circle;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;


/**
 * A pathfinder that uses tangent lines to obstacles to find the path to the
 * destination.
 */
public class ObstacleTangentPathfinder implements Pathfinder {
	
	/**
	 * Structure for storing a tangent point to an obstacle.
	 */
	private class ObstacleTangent {
		private Circle obstacle;
		private Point2D.Double tangentPoint;
		
		/**
		 * Create a new obstacle tangent.
		 * 
		 * @param obstacle Obstacle.
		 * @param tangentPoint Obstacle's tangent point.
		 */
		public ObstacleTangent(Circle obstacle, Point2D.Double tangentPoint) {
			this.obstacle = obstacle;
			this.tangentPoint = tangentPoint;
		}

		/**
		 * Get the obstacle.
		 * 
		 * @return The obstacle
		 */
		public final Circle getObstacle() {
			return obstacle;
		}

		/**
		 * Get the obstacle's tangent point.
		 * 
		 * @return The tangent point.
		 */
		public final Point2D.Double getTangentPoint() {
			return tangentPoint;
		}
	}
	
	
	/** The amount, by which the collision points are pushed from obstacles. */
	private static double COLLISION_ADJUSTMENT = 10.0;
	/** The amount by which obstacles are increased in extraction. */
	private static final double OBSTACLE_SIZE_INCREASE = Robot.LENGTH_CM * 0.7;
	
	/** A fallback pathfinder. */
	private HeuristicPathfinder fallback = new HeuristicPathfinder();
	
	
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
	 * Calculate all obstacle tangents for the given set of obstacles and a
	 * point.
	 * 
	 * @param obstacles Obstacles to test against.
	 * @param point Point of interest.
	 * @return A set of obstacle tangents.
	 */
	private ArrayList<ObstacleTangent> getObstacleTangents(ArrayList<Circle> obstacles,
			Point2D.Double point) {
		ArrayList<ObstacleTangent> tangents = new ArrayList<ObstacleTangent>();
		
		for (Circle obs : obstacles) {
			Point2D.Double tangentPoints[] = GeomUtils.circleTangentPoints(obs, point);
			if (tangentPoints == null) {
				continue;
			}
			
			for (Point2D.Double tanPt : tangentPoints) {
				boolean pointGood = true;

				for (Circle obs1 : obstacles) {
					if (obs == obs1) {
						continue;
					}
					if (GeomUtils.lineIntersectsCircle(point, tanPt, obs1)) {
						pointGood = false;
					}
				}
				
				if (pointGood) {
					tangents.add(new ObstacleTangent(obs, tanPt));
				}
			}
		}
		
		return tangents;
	}
	
	
	private double createPath(ArrayList<Waypoint> path,
			Point2D.Double points[], double startAngle) {
		path.clear();
		
		double pathLength = 0;
		for (int i = points.length - 2; i >= 0; --i) {
			double angle = startAngle;
			if (i > 0) {
				Vector2D dirVec = Vector2D.subtract(
						new Vector2D(points[i]), new Vector2D(points[i-1]));
				angle = dirVec.getDirection();
			}
			
			pathLength += GeomUtils.pointDistance(points[i], points[i+1]);
			path.add(0, new Waypoint(new Vector2D(points[i]), angle,
					new Vector2D(points[i+1]), pathLength, (i == (points.length - 2))));
		}
		
		return pathLength;
	}
	
	/**
	 * Calculate a path from source and destination obstacle tangents.
	 * 
	 * @param startPos Starting position.
	 * @param startAngle Starting direction.
	 * @param startTangents Source obstacle tangents.
	 * @param destTangents Destination obstacle tangents.
	 * @param destPos Final destination.
	 * @return A set of waypoints that will lead the robot to the destination.
	 */
	private ArrayList<Waypoint> getPathFromTangents(Point2D.Double startPos, 
			double startAngle, ArrayList<ObstacleTangent> startTangents, 
			ArrayList<ObstacleTangent> destTangents, Point2D.Double destPos) {
		ArrayList<Waypoint> bestPath = new ArrayList<Waypoint>();
		double bestPathLength = Double.MAX_VALUE;
		
		ArrayList<Waypoint> curPath = new ArrayList<Waypoint>();
		
		for (ObstacleTangent startTan : startTangents) {
			for (ObstacleTangent destTan : destTangents) {
				curPath.clear();
				double curPathLength = 0.0;
				
				if (startTan.getTangentPoint().equals(destTan.getTangentPoint())) {
					Point2D.Double points[] = {
							startPos, startTan.getTangentPoint(), destPos
					};
					createPath(curPath, points, startAngle);
				} else {
					Point2D.Double startObsPoint = GeomUtils.getClosestPointToLine(
							startTan.getObstacle().getCentre(), startTan.getTangentPoint(),
							destTan.getTangentPoint());
					startObsPoint = GeomUtils.changePointDistanceToCircle(
							startTan.getObstacle(), startObsPoint,
							startTan.getObstacle().getRadius());
					
					Point2D.Double destObsPoint = GeomUtils.getClosestPointToLine(
							destTan.getObstacle().getCentre(), startTan.getTangentPoint(),
							destTan.getTangentPoint());
					destObsPoint = GeomUtils.changePointDistanceToCircle(
							destTan.getObstacle(), destObsPoint,
							destTan.getObstacle().getRadius());
					
					if ((startTan.getObstacle().getCentre().equals(destTan.getObstacle().getCentre()))
							|| (startObsPoint.equals(destObsPoint))) {
						Vector2D startToDest = Vector2D.subtract(
								new Vector2D(destTan.getTangentPoint()),
								new Vector2D(startTan.getTangentPoint()));
						destObsPoint = Vector2D.add(new Vector2D(startObsPoint), startToDest);
					}
					
					Point2D.Double midpoint1 = GeomUtils.getLineLineIntersection(
							startPos, startTan.getTangentPoint(), startObsPoint,
							destObsPoint);
					Point2D.Double midpoint2 = GeomUtils.getLineLineIntersection(
							destPos, destTan.getTangentPoint(), startObsPoint,
							destObsPoint);
					
					if ((midpoint1 == null) || (midpoint2 == null)
							|| (!WorldState.isPointInPaddedPitch(midpoint1, Robot.LENGTH_CM / 2))
							|| (!WorldState.isPointInPaddedPitch(midpoint2, Robot.LENGTH_CM / 2))) {
						continue;
					}
					
					if (GeomUtils.isPointLeftOfVector(destPos, midpoint1, midpoint2)) {
						Point2D.Double points[] = {
								startPos, midpoint1, destPos
						};
						createPath(curPath, points, startAngle);
					} else {
						Point2D.Double points[] = {
								startPos, midpoint1, midpoint2, destPos
						};
						createPath(curPath, points, startAngle);
					}
				}
				
				if (curPathLength < bestPathLength) {
					bestPathLength = curPathLength;
					bestPath = new ArrayList<Waypoint>(curPath);
				}
			}
		}
		
		System.out.println(bestPath.size());
		return bestPath;
	}


	/**
	 * @see sdp.AI.pathfinding.Pathfinder#getPath(sdp.AI.AIWorldState, java.awt.geom.Point2D.Double, boolean)
	 */
	@Override
	public ArrayList<Waypoint> getPath(AIWorldState worldState, Point2D.Double dest,
			boolean ballIsObstacle) {
		// Convenience variable initialisation.
		int obstacleFlags = WorldState.makeObstacleFlagsForOpponent(ballIsObstacle,
				worldState.isOwnTeamBlue());
		
		Vector2D startVec = new Vector2D(worldState.getOwnRobot().getCoords());
		Vector2D destVec = new Vector2D(dest);
		
		Vector2D startToDest = Vector2D.subtract(destVec, startVec);
		
		// Get obstacles out of the world state.
		ArrayList<Circle> obstacles = WorldState.getObstacleCircles(worldState, obstacleFlags);
		for (Circle c : obstacles) {
			c.setRadius(c.getRadius() + OBSTACLE_SIZE_INCREASE);
		}
		
		// Check for a direct path solution.
		if (WorldState.isDirectPathClear(worldState, startVec, destVec, obstacleFlags)) {
			ArrayList<Waypoint> path = new ArrayList<Waypoint>();
			path.add(new Waypoint(startVec, worldState.getOwnRobot().getAngle(),
					destVec, startToDest.getLength(), true));
			return path;
		}
		
		// Adjust points.
		Vector2D startVecAdj = movePointOutOfObstacle(obstacles, startVec);
		Vector2D destVecAdj = movePointOutOfObstacle(obstacles, destVec);
		
		// If one does not exist, get obstacle tangents.
		ArrayList<ObstacleTangent> startTangents = getObstacleTangents(obstacles, startVecAdj);
		ArrayList<ObstacleTangent> destTangents = getObstacleTangents(obstacles, destVec);
		
		// Compute the path from the obstacle tangents.
		ArrayList<Waypoint> path = getPathFromTangents(startVecAdj,
				worldState.getOwnRobot().getAngle(), startTangents,
				destTangents, destVecAdj);
		if (path.size() == 0) {
			Painter.fullPath = new ArrayList<Waypoint>();
			return fallback.getPath(worldState, dest, ballIsObstacle);
		} else {
			Painter.fullPath = path;
			return path;
		}
	}

}
