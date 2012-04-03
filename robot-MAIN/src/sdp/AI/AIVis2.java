package sdp.AI;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.text.Utilities;

import com.googlecode.javacv.FrameGrabber.Array;

import lejos.robotics.navigation.WayPoint;

import sdp.AI.AIVisualServoing.AttackMode;
import sdp.AI.pathfinding.HeuristicPathfinder;
import sdp.AI.pathfinding.Waypoint;
import sdp.common.DeprecatedCode;
import sdp.common.FPSCounter;
import sdp.common.Painter;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;

public class AIVis2 extends AIVisualServoing {
	
	
	private final static boolean USE_OLD_STYLE_DIRECTION = true;
	
	/** At what distance fromt the robot will be the point on the path that we want to follow */
	private final static double BEZIER_FRACTION = 0.5;
	
	private final static double GUARD_BALL_DIST = 30;
	
	private final static double MAX_HESITATION_FPS = 1.5;
	
	private final FPSCounter fps = new FPSCounter();
	
	private Boolean lastLeft = null;
	
	
	private HeuristicPathfinder path = new HeuristicPathfinder();
	
	private Command play() {
		
		if (canWeAttack(aiWorldState)) {
			try {
				return gotBall();
			} catch (IOException e) {
				return null;
			}
		}
		
		final Vector2D robot = new Vector2D(aiWorldState.getOwnRobot().getCoords());
		
		double x_sub = aiWorldState.isOwnGoalLeft() ? -2 : 2;
		final Vector2D ball = new Vector2D(aiWorldState.getBallCoords());
		Vector2D target = new Vector2D(ball.x+x_sub, ball.y);
		
		Vector2D optimal;
		
		if (USE_OLD_STYLE_DIRECTION) {
			Point2D.Double optimalPoint = null;
			for (int i = 0; i < OPTIMAL_POINT_SEARCH_TRIES; i++) {
				optimalPoint = getOptimalAttackPoint(aiWorldState, optimalPointOffset, AttackMode.Full);
				
				if (optimalPoint == null) {
					optimalPointOffset *= OPTIMAL_POINT_ADJUST;
				} else {
					break;
				}
			}
			optimal = optimalPoint == null ? null : new Vector2D(optimalPoint);
		} else {
			final Point2D.Double optimalPt = getOptimalAttackPoint(aiWorldState, 2, AttackMode.Full);
			optimal = optimalPt == null ? null : new Vector2D(optimalPt);
		}
		
		final double finalAngle = optimal != null ? getDirection(optimal, ball) : (aiWorldState.isOwnGoalLeft() ? 0 : 180);
		
		final WayPt direct = new WayPt();
		direct.loc = optimal == null ? target : optimal;
		direct.dir = finalAngle;
		
		final WayPt avoid = go(direct);
	
		final Vector2D secondary = getNextBezierPoint(avoid, WorldState.makeObstacleFlagsForOpponent(true, aiWorldState.isOwnTeamBlue()));
		final double dist = Vector2D.subtract(robot, secondary).getLength();
		
		final ArrayList<Waypoint> retValue = new ArrayList<Waypoint>();
		retValue.add(new Waypoint(robot, aiWorldState.getOwnRobot().getAngle(), secondary, dist, true));
		
		Painter.target = new Vector2D[]{avoid.loc};
		Painter.lines = new Vector2D[]{robot, avoid.loc, avoid.loc, direct.loc, direct.loc, Vector2D.add(direct.loc, Vector2D.rotateVector(new Vector2D(30, 0), direct.dir)), avoid.loc, Vector2D.add(avoid.loc, Vector2D.rotateVector(new Vector2D(30, 0), avoid.dir))};
		Painter.targetSecondary = new Vector2D[]{secondary};
		
		
		final Command comm = getWaypointCommand(retValue, false, DRIVING_SPEED_MULTIPLIER,
					STOP_TURN_THRESHOLD);
		
//		double distSec = Vector2D.subtract(secondary, robot).getLength();
//		double t = distSec / Robot.MAX_DRIVING_SPEED;
//		double ang = getTurningAmount(robot, secondary, aiWorldState.getOwnRobot().getAngle());
//		
//		Command comm = new Command(Robot.MAX_DRIVING_SPEED, ang/t, false);
		
		guardBall(comm);

		return comm;
	}
	
	/**
	 * Prevents us kicking the ball into our own goal
	 */
	private void guardBall(final Command comm) {

		final Vector2D ball = new Vector2D(aiWorldState.getBallCoords());
		final Vector2D robot = new Vector2D(aiWorldState.getOwnRobot().getCoords());
		final double dist = Vector2D.subtract(ball, robot).getLength();
		final double ballturn = Math.abs(getTurningAmount(robot, ball, aiWorldState.getOwnRobot().getAngle()));
		final double balldir = Math.abs(Vector2D.subtract(ball, robot).getDirection());
		
		if (dist < GUARD_BALL_DIST) {

			if ((comm.drivingSpeed > 0 && ballturn < 70) ||
					(comm.drivingSpeed < 0 && ballturn > 110)) {

				if (aiWorldState.isOwnGoalLeft()) {
					if (ball.getX() < robot.getX()) {
						System.out.println("Preventing kick ball in own goal left "+ballturn);
						comm.drivingSpeed = 0;//-comm.drivingSpeed;
					}
				} else {
					if (ball.getX() > robot.getX()) {
						System.out.println("Preventing kick ball in own goal right"+ballturn);
						comm.drivingSpeed = 0;//-comm.drivingSpeed;
					}
				}

			}
		}

	}
	
	private Vector2D getNextBezierPoint(final WayPt waypoint, final int obstacles) {
		
		final double pathsize = Vector2D.subtract(waypoint.loc, new Vector2D(aiWorldState.getOwnRobot().getCoords())).getLength();
		final double coeff = pathsize/2;
		final double min_dist = pathsize*BEZIER_FRACTION;
		
		final double turningAmount = getTurningAmount(new Vector2D(aiWorldState.getOwnRobot().getCoords()), waypoint.loc, aiWorldState.getOwnRobot().getAngle());
		final boolean goingBackwards = Math.abs(turningAmount) > 90;	
		
		final Vector2D pt1 = new Vector2D(aiWorldState.getOwnRobot().getCoords());
		final double robAng = aiWorldState.getOwnRobot().getAngle();
		final Vector2D robot = new Vector2D(aiWorldState.getOwnRobot().getCoords());
		final Vector2D robDir = Vector2D.rotateVector(new Vector2D(coeff, 0), goingBackwards ? GeomUtils.normaliseAngle(robAng - 180) : robAng);
		final Vector2D cp1 = Vector2D.add(pt1, robDir);
		final Vector2D pt2 = waypoint.loc;
		final Vector2D cp2 = Vector2D.subtract(pt2, Vector2D.rotateVector(new Vector2D(coeff, 0), waypoint.dir));
		
		final CubicCurve2D.Double bezier = new CubicCurve2D.Double(pt1.x, pt1.y, cp1.x, cp1.y, cp2.x, cp2.y, pt2.x, pt2.y);
		
		final FlatteningPathIterator pi = new FlatteningPathIterator(bezier.getPathIterator(null), 0.1);
		
		Vector2D result = null;
		
		final ArrayList<Vector2D> vecs = new ArrayList<Vector2D>();
		double pathsofar = 0;
		Vector2D prevVector = null;
		while (!pi.isDone()) {  
			
			final double[] coordinates = new double[6];
			pi.currentSegment(coordinates);

		    final Vector2D vector = new Vector2D(coordinates[0], coordinates[1]);
		    
		    
		    if (prevVector == null)
		    	prevVector = vector;
		    
		    pathsofar += Vector2D.subtract(prevVector, vector).getLength();
		    
		    if (pathsofar > min_dist && result == null &&
		    		(
		    		WorldState.isDirectPathClear(aiWorldState, robot, vector, obstacles)
		    		|| Vector2D.subtract(vector, robot).getLength() < 10))
		    	result = vector;
		    
		    prevVector = vector;
		    vecs.add(vector);
		    
			pi.next();
		}
		
		Painter.linesSecondary = vecs.toArray(new Vector2D[0]);
		
		return result == null ? prevVector : result;
		
		
	}
	
	// pathfinding
	
	/**
	 * Sets a waypoint that we need to get directly to
	 * @param target
	 * @return returns the waypoint that we need to go to in order to avoid the obstacles on the way
	 */
	private WayPt go(final WayPt target) {
		
		final Vector2D robot = new Vector2D(aiWorldState.getOwnRobot().getCoords());
		final int obstacleFlags = WorldState.makeObstacleFlagsForOpponent(true, aiWorldState.isOwnTeamBlue());
		
		if (WorldState.isDirectPathClear(aiWorldState, robot, target.loc, obstacleFlags)) {
			return target;
		}
		
		final WayPt left = returnFirstDir(target, 3, 10, obstacleFlags, robot);
		final WayPt right = returnFirstDir(target, -3, 10, obstacleFlags, robot);
		
		if (left == null || right == null) {
			
			Painter.linesSecondary = null;

			if (left == null && right == null) {
				final Waypoint wayp = path.getPath(aiWorldState, target.loc, true).get(0);
				final WayPt wp = new WayPt();
				wp.loc = wayp.getTarget();
				wp.dir = wayp.getTurningAngle();
				return wp;
			}
			else if (right == null)
				return left;
			else if (left == null)
				return right;

		}
		
		// left path turning cost
		
		final double finalturn = Math.abs(getTurningAmount(target.loc, Vector2D.multiply(target.loc, 2), target.dir)),
				finalpath = Vector2D.subtract(left.loc, target.loc).getLength();
		
		final double turn1left = Math.abs(getTurningAmount(robot, left.loc, aiWorldState.getOwnRobot().getAngle())),
			turn2left = Math.abs(getTurningAmount(left.loc, target.loc, getDirection(left.loc, target.loc))),
			turnleft = turn1left + turn2left + finalturn,
			pathleft = Vector2D.subtract(robot, left.loc).getLength() + finalpath,
			timeleft = Math.max(turnleft/Robot.MAX_TURNING_SPEED, pathleft/Robot.MAX_DRIVING_SPEED);
		
		final double turn1right = Math.abs(getTurningAmount(robot, right.loc, aiWorldState.getOwnRobot().getAngle())),
			turn2right = Math.abs(getTurningAmount(right.loc, target.loc, getDirection(right.loc, target.loc))),
			turnright = turn1right + turn2right + finalturn,
			pathright = Vector2D.subtract(robot, right.loc).getLength() + finalpath,
			timeright = Math.max(turnright/Robot.MAX_TURNING_SPEED, pathright/Robot.MAX_DRIVING_SPEED);
		
		//final Vector2D[] leftLines = new Vector2D[]{new Vector2D(aiWorldState.getOwnRobot().getCoords()), left.loc, left.loc, target.loc, target.loc, Vector2D.add(target.loc, Vector2D.rotateVector(new Vector2D(30, 0), target.dir))};
		//final Vector2D[] rightLines =  new Vector2D[]{new Vector2D(aiWorldState.getOwnRobot().getCoords()), right.loc, right.loc, target.loc, target.loc, Vector2D.add(target.loc, Vector2D.rotateVector(new Vector2D(30, 0), target.dir))};
		
		//Painter.linesSecondary = timeleft < timeright ? rightLines : leftLines;
		
		boolean isleft = timeleft < timeright;
		
		if (lastLeft == null || lastLeft != isleft) {
			fps.tick();
			lastLeft = isleft;
		}
		
		if (fps.getFPS() > MAX_HESITATION_FPS) {
			isleft = lastLeft;
		}
		
		return isleft ? left : right;
	}
	
	private static final double getTurningAmount(final Vector2D origin, final Vector2D target, final double initDir) {
		return Vector2D.rotateVector(Vector2D.subtract(target, origin), -initDir).getDirection();
	}
	
	private static final double getDirection(final Vector2D from, final Vector2D to) {
		return Vector2D.subtract(to, from).getDirection();
	}
	
	private WayPt returnFirstDir(final WayPt targ, double angCh, double pathCoeff, int obstacles, final Vector2D robot) {
		double total = 0;
		
		final double absCh = Math.abs(angCh);
		final double targetDir = Vector2D.subtract(targ.loc, robot).getDirection();
		
		
		for (double ang = targetDir; total < 180; total += absCh, ang = GeomUtils.normaliseAngle(ang+angCh)) {
			Vector2D angDirV = Vector2D.rotateVector(new Vector2D(1, 0), ang);
			final double sec = DeprecatedCode.raytraceVector(aiWorldState, robot, angDirV, aiWorldState.isOwnTeamBlue(), true).getLength();
			while ((angDirV = Vector2D.changeLength(angDirV, angDirV.getLength()+pathCoeff)).getLength() < sec) {
				final Vector2D target = Vector2D.add(robot, angDirV);
				if (WorldState.isDirectPathClear(aiWorldState, target, targ.loc, obstacles) &&
						WorldState.isDirectPathClear(aiWorldState, robot, target, obstacles)) {
					WayPt wp = new WayPt();
					wp.loc = target;
					wp.dir = Vector2D.subtract(targ.loc, target).getDirection();
					return wp;
				}
			}
		}
		
		return null;
	}
	
	
	
	// inner
	
	private static class WayPt {
		
		public double dir;
		public Vector2D loc;
		
	}
	
	// overrides
	
	@Override
	protected Command chaseBall() throws IOException {
		return play();
	}
	
	@Override
	protected Command defendGoal() throws IOException {
		return play();
	}
	
	

}
