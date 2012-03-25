package sdp.common;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;

import sdp.AI.AIWorldState;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Goal;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;


/**
 * A container for all deprecated functionality. These functions are present
 * here to not clutter the clean code and not break all code that depends on
 * them. The code that depends of these functions should be updated as quickly
 * as possible.
 */
public class DeprecatedCode {

	/**
	 * Starting from origin in the given direction, find the first point of collision in the scene
	 * 
	 * TODO: Replace with getClosestCollisionVec.
	 * 
	 * @param origin the start of the vector
	 * @param direction size doesn't matter, only the angle is relevant
	 * @param ignore_blue true to ignore blue robot, false to ignore yellow, null to include both
	 * @return a {@link Vector2D} in the same direction as direction but with greater length (distance from origin to the nearest collision point, raytraced along direction's direction)
	 */
	@Deprecated
	public static Vector2D raytraceVector(WorldState ws, Vector2D origin, Vector2D direction, Boolean ignore_blue, boolean include_ball_as_obstacle) {
		if (origin.getX() <= 0 || origin.getY() <= 0 || origin.getX() >= WorldState.PITCH_WIDTH_CM || origin.getY() >= WorldState.PITCH_HEIGHT_CM)
			return Vector2D.ZERO();
		Vector2D near;
		Vector2D temp = GeomUtils.getLocalRaySegmentIntersection(origin, direction, new Vector2D(0, 0), new Vector2D(WorldState.PITCH_WIDTH_CM, 0));
		near = temp;
		temp = GeomUtils.getLocalRaySegmentIntersection(origin, direction, new Vector2D(WorldState.PITCH_WIDTH_CM, 0), new Vector2D(WorldState.PITCH_WIDTH_CM, WorldState.PITCH_HEIGHT_CM));
		if (temp != null && (near == null || temp.getLength() < near.getLength()))
			near = temp;
		temp = GeomUtils.getLocalRaySegmentIntersection(origin, direction, new Vector2D(WorldState.PITCH_WIDTH_CM, WorldState.PITCH_HEIGHT_CM), new Vector2D(0, WorldState.PITCH_HEIGHT_CM));
		if (temp != null && (near == null || temp.getLength() < near.getLength()))
			near = temp;
		temp = GeomUtils.getLocalRaySegmentIntersection(origin, direction, new Vector2D(0, WorldState.PITCH_HEIGHT_CM), new Vector2D(0, 0));
		if (temp != null && (near == null || temp.getLength() < near.getLength()))
			near = temp;
		// collision with a Robot
		for (int i = 0; i <= 1; i++) {
			if (ignore_blue != null && ((ignore_blue ? 0 : 1) == i))
				continue;
			Robot robot = i == 0 ? ws.getBlueRobot() : ws.getYellowRobot();
			temp = GeomUtils.getLocalRaySegmentIntersection(origin, direction, new Vector2D(robot.getFrontLeft()), new Vector2D(robot.getFrontRight()));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
			temp = GeomUtils.getLocalRaySegmentIntersection(origin, direction, new Vector2D(robot.getFrontRight()), new Vector2D(robot.getBackRight()));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
			temp = GeomUtils.getLocalRaySegmentIntersection(origin, direction, new Vector2D(robot.getBackRight()), new Vector2D(robot.getBackLeft()));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
			temp = GeomUtils.getLocalRaySegmentIntersection(origin, direction, new Vector2D(robot.getBackLeft()), new Vector2D(robot.getFrontLeft()));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
		}
		// collision with ball
		if (include_ball_as_obstacle) {
			Vector2D ball = new Vector2D(ws.getBallCoords());
			temp = GeomUtils.getLocalRaySegmentIntersection(origin, direction, new Vector2D(ball.getX(), ball.getY()-DeprecatedCode.SIZE_OF_BALL_OBSTACLE/2), new Vector2D(ball.getX(), ball.getY()+DeprecatedCode.SIZE_OF_BALL_OBSTACLE/2));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
		}
		if (near != null) 
			return near;
		return
				Vector2D.changeLength(direction, WorldState.PITCH_WIDTH_CM);
	}

	/**
	 * TODO: Replace with getClosestCollisionVec.
	 * 
	 * @param ws
	 * @param robot
	 * @param local_origin In robot's coordinate system. Make sure it is outside robot to avoid false readings!
	 * @param local_direction In robot's coordinate system
	 * @return same output as {@link raytraceVector} - in table coordinates
	 */
	@Deprecated
	public static Vector2D raytraceVector(WorldState ws, Robot robot, Vector2D local_origin, Vector2D local_direction, boolean include_ball_as_obstacle) {
		return DeprecatedCode.raytraceVector(ws, robot, local_origin, local_direction, null, include_ball_as_obstacle);
	}

	/**
	 * Raytrace vector with relation to a robot
	 * 
	 * TODO: Replace with getClosestCollisionVec or an overloaded function.
	 * 
	 * @param ws
	 * @param robot
	 * @param local_origin In robot's coordinate system. Make sure it is outside robot to avoid false readings!
	 * @param local_direction In robot's coordinate system
	 * @param am_i_blue if true, ignores blue if false ignores yellow. To include all robots, use {@link raytraceVector}
	 * @return same output as {@link raytraceVector} - in table coordinates
	 */
	@Deprecated
	public static Vector2D raytraceVector(WorldState ws, Robot robot, Vector2D local_origin, Vector2D local_direction,  Boolean am_i_blue, boolean include_ball_as_obstacle) {
		Vector2D origin = Robot.getGlobalVector(robot, local_origin);
		Vector2D direction = Vector2D.subtract(origin, Robot.getGlobalVector(robot, local_direction));
		return raytraceVector(ws, origin, direction, am_i_blue, include_ball_as_obstacle);
	}
	

	/**
	 * Calculates all the points behind the ball that would align the robot to shoot and
	 * returns the point closest to the robot.
	 * 
	 * TODO: Replace with getOptimalPoint.
	 * 
	 * @return The point closest to the robot that would allow it to shoot.
	 * @throws NullPointerException Throws exception when the robot can't see a goal.
	 */
	@Deprecated
	public static Point2D.Double getOptimalPointBehindBall(AIWorldState ws, double point_offset) {
		Goal enemy_goal = new Goal(ws.getEnemyGoal().getCentre(), true);
		Robot robot = ws.getOwnRobot();
		Robot enemy_robot = ws.getEnemyRobot();
	
		ArrayList<Point2D.Double> goal_points = new ArrayList<Point2D.Double>();
		Point2D.Double min_point = null;
		double min_distance = WorldState.PITCH_WIDTH_CM*2;
	
		goal_points.add(enemy_goal.getCentre());
		goal_points.add(enemy_goal.getTop());
		goal_points.add(enemy_goal.getBottom());
		goal_points.add(new Point2D.Double(enemy_goal.getCentre().x, enemy_goal.getCentre().y - WorldState.PITCH_HEIGHT_CM));
		goal_points.add(new Point2D.Double(enemy_goal.getCentre().x, enemy_goal.getCentre().y + WorldState.PITCH_HEIGHT_CM));
	
		Iterator<Point2D.Double> itr = goal_points.iterator();
		while (itr.hasNext()) {
			Point2D.Double point = itr.next();
	
			if (point.y < 0) {
				if (!Utilities.lineIntersectsRobot(point, ws.getBallCoords(),
						enemy_robot.getTopImage())) {
					itr.remove();
	
				}
			} else if (point.y > WorldState.PITCH_HEIGHT_CM) {
				if (!Utilities.lineIntersectsRobot(point, ws.getBallCoords(),
						enemy_robot.getBottomImage())) {
					itr.remove();
	
				}
			} else {
	
				if (!Utilities.lineIntersectsRobot(point, ws.getBallCoords(),
						enemy_robot) || point.x > WorldState.PITCH_WIDTH_CM) {
					itr.remove();
	
				}
	
			}
	
		}
	
		//System.out.println("begin printing points");
	
		itr = goal_points.iterator();
		while (itr.hasNext()) {
			Point2D.Double point = itr.next();
			Point2D.Double temp_point = Utilities.getPointBehindBall(point, ws.getBallCoords(),ws.isOwnGoalLeft(), point_offset);
	
			//System.out.println(temp_point);
			
			if (DeprecatedCode.isPointInField(temp_point)) { 
				if (!Robot.isPointAroundRobot(temp_point, enemy_robot) && Utilities.lineIntersectsRobot(temp_point, ws.getBallCoords(),
						enemy_robot)) {
					//System.out.println(Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength());
					//System.out.println("Min distance: "+min_distance);
					if (Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength() < min_distance) {
						min_point = temp_point;
						min_distance = Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength();
					}
				} 
			} 
		} 
		//System.out.println("min point "+min_point); //+ "enemy robot" + enemy_robot.getCoords() + "angle" + enemy_robot.getAngle() + "ball coords "+ws.getBallCoords());
		return min_point;
	}

	/**
	 * Calculates all the points behind the ball that would align the robot to shoot and
	 * returns the point closest to the robot.
	 * It does not take into account the image goals
	 * 
	 * TODO: Replace with getOptimalPoint.
	 * 
	 * @param ws
	 * @param my_goal_left
	 * @param my_team_blue
	 * @param priority - true is shoot only with walls, false is shoot only with main goal
	 * @return the optimal point from which it can shoot
	 * @throws NullPointerException
	 */
	@Deprecated
	public static Point2D.Double getOptimalPointBehindBall(AIWorldState ws, double point_offset, boolean wall_priority) throws NullPointerException {
		
		Goal enemy_goal = ws.getEnemyGoal();
		Robot robot = ws.getOwnRobot();
		Robot enemy_robot = ws.getEnemyRobot();
		
		ArrayList<Point2D.Double> goal_points = new ArrayList<Point2D.Double>();
		Point2D.Double min_point = null;
		double min_distance = WorldState.PITCH_WIDTH_CM*2;
		
		if (wall_priority){
			//add only imaginary goals 
			goal_points.add(new Point2D.Double(enemy_goal.getCentre().x, enemy_goal.getCentre().y - WorldState.PITCH_HEIGHT_CM));
			goal_points.add(new Point2D.Double(enemy_goal.getCentre().x, enemy_goal.getCentre().y + WorldState.PITCH_HEIGHT_CM));
		}
		
		else {
		
			//add only points in mail goal
			if (ws.isOwnGoalLeft()) {
				enemy_goal = new Goal(new Point2D.Double(WorldState.PITCH_WIDTH_CM , WorldState.GOAL_CENTRE_Y ));
			} else {
				enemy_goal = new Goal(new Point2D.Double(0 , WorldState.GOAL_CENTRE_Y ));
			}
		
			double offset = enemy_goal.getBottom().y - enemy_goal.getTop().y; //offset>0
			offset = offset/5;
	
			goal_points.add(enemy_goal.getCentre());
			goal_points.add(enemy_goal.getTop());
			goal_points.add(enemy_goal.getBottom());
	
			for (int i=0; i<4; i++)
				goal_points.add(new Point2D.Double(enemy_goal.getTop().x, enemy_goal.getTop().y - (i+1)*offset));
	
		}
		
		/* start calculating the optimal point */
		
		Iterator<Point2D.Double> itr = goal_points.iterator();
		while (itr.hasNext()) {
			Point2D.Double point = itr.next();
	
			if (!Utilities.lineIntersectsRobot(point, ws.getBallCoords(), enemy_robot)) 
				itr.remove();
	
		}
	
		itr = goal_points.iterator();
		while (itr.hasNext()) {
			Point2D.Double point = itr.next();
			Point2D.Double temp_point = Utilities.getPointBehindBall(point, ws.getBallCoords(), ws.isOwnGoalLeft(), point_offset);
	
			if (DeprecatedCode.isPointInField(temp_point)) { 
				if (!Robot.isPointAroundRobot(temp_point, enemy_robot)) {
					//System.out.println(Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength());
					//System.out.println("Min distance: "+min_distance);
					if (Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength() < min_distance) {
						min_point = temp_point;
						min_distance = Vector2D.subtract(new Vector2D(temp_point), new Vector2D(robot.getCoords())).getLength();
					}
				}
			}
		}
	
	
		return min_point;
	}
	

	/**
	 * Whether there is direct visibility from a point of the pitch to another one.
	 * 
	 * TODO: Replace with getClosestCollisionDist or an overloaded function.
	 * 
	 * @param ws
	 * @param robot
	 * @param startPt
	 * @param endPt
	 * @return
	 */
	@Deprecated
	public static boolean visibility(WorldState ws, Vector2D endPt, boolean am_i_blue, boolean include_ball_as_obstacle) {
		Robot robot = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot(); 
		Vector2D startPt = new Vector2D(robot.getCoords());
		Vector2D dir =  Vector2D.subtract(endPt, startPt);
		Vector2D ray = raytraceVector(ws, startPt, dir, am_i_blue, include_ball_as_obstacle);
		return ray.getLength() >= dir.getLength();
	}


	/**
	 * Returns the distance to the direct collision in a given direction.
	 * 
	 * TODO: Replace with getClosestCollisionDist.
	 * 
	 * @param ws
	 * @param robot
	 * @param startPt
	 * @param endPt
	 * @return
	 */
	@Deprecated
	public static double visibility2(WorldState ws, Vector2D endPt, boolean am_i_blue, boolean include_ball_as_obstacle) {
		Robot robot = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot(); 
		Vector2D startPt = new Vector2D(robot.getCoords());
		Vector2D dir =  Vector2D.subtract(endPt, startPt);
		Vector2D ray = raytraceVector(ws, startPt, dir, am_i_blue, include_ball_as_obstacle);
		return ray.getLength();
	}


	/**
	 * Whether you could reach this point if you turn into its direction and go into straight line.
	 * 
	 * TODO: Replace this function with isDirectPathClear.
	 * 
	 * @param ws
	 * @param robot
	 * @param startPt
	 * @param endPt
	 * @return
	 */
	@Deprecated
	public static boolean reachability(WorldState ws, Vector2D endPt, boolean am_i_blue, boolean include_ball_as_obstacle, double coeff) {
		if (!visibility(ws, endPt, am_i_blue, include_ball_as_obstacle))
			return false;
		Robot robot = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot(); 
		Vector2D startPt = new Vector2D(robot.getCoords());
		Vector2D dir =  Vector2D.subtract(endPt, startPt);
		double dir_l = dir.getLength();
		double angle = (-Vector2D.getDirection(dir)+90)*Math.PI/180d;
		final double length = Robot.LENGTH_CM/2-1;
		double cos = Math.cos(angle)*length;
		double sin = Math.sin(angle)*length;
		Vector2D left = new Vector2D(cos, sin);
		Vector2D right = new Vector2D(-cos, -sin);
	
		Vector2D ray_left = raytraceVector(ws, Vector2D.add(startPt, left), dir, am_i_blue, include_ball_as_obstacle);
		if (ray_left.getLength() < dir_l)
			return false;
		Vector2D ray_right = raytraceVector(ws, Vector2D.add(startPt, right), dir, am_i_blue, include_ball_as_obstacle);
		if (ray_right.getLength() < dir_l)
			return false;
		
		Vector2D left2 = new Vector2D(cos*coeff, sin*coeff);
		Vector2D global_left2 = Vector2D.add(startPt, left2);
		Vector2D right2 = new Vector2D(-cos*coeff, -sin*coeff);
		Vector2D global_right2 = Vector2D.add(startPt, right2);
		
		Vector2D ray_left2 = raytraceVector(ws, global_left2, dir, am_i_blue, include_ball_as_obstacle);
		if (ray_left2.getLength() < dir_l)
			return false;
		Vector2D ray_right2 = raytraceVector(ws, global_right2, dir, am_i_blue, include_ball_as_obstacle);
		if (ray_right2.getLength() < dir_l)
			return false;
		
		Vector2D dir_left = Vector2D.subtract(endPt, global_left2);
		Vector2D ray_left3 = raytraceVector(ws, global_left2, dir_left, am_i_blue, include_ball_as_obstacle);
		if (ray_left3.getLength() < dir_l)
			return false;
		
		Vector2D dir_right = Vector2D.subtract(endPt, global_right2);
		Vector2D ray_right3 = raytraceVector(ws, global_right2, dir_right, am_i_blue, include_ball_as_obstacle);
		if (ray_right3.getLength() < dir_l)
			return false;
	
		
		return true;
	}

	/**
	 * Calculates if the given point is within the field.
	 * Includes an offset equal to half of the length of the robot, to allow
	 * it to get behind the ball
	 * 
	 * TODO: Replace with isPointInPaddedPitch(point, Robot.LENGTH_CM / 2).
	 * 
	 * @param point The point to be checked
	 * @return True if the point is within the bounds of the field.
	 */
	@Deprecated
	public static boolean isPointInField(Point2D.Double point) {
		double offset = Robot.LENGTH_CM / 2;
		if (point.getX() >= offset && point.getX() <= (WorldState.PITCH_WIDTH_CM - offset)) {
			if (point.getY() >= offset && point.getY() <= (WorldState.PITCH_HEIGHT_CM - offset)) {
				return true;
			}
		}
		return false;
	}

	/** Size of the ball obstacle. */
	public static final double SIZE_OF_BALL_OBSTACLE = Robot.LENGTH_CM;

}
