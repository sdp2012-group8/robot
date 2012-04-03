package sdp.AI;

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
import sdp.common.Painter;
import sdp.common.geometry.GeomUtils;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;

public class AIVis2 extends AIVisualServoing {
	
	
	private final static boolean USE_OLD_STYLE_DIRECTION = true;
	
	private HeuristicPathfinder path = new HeuristicPathfinder();
	
	private Command play() {
		
		if (canWeAttack(aiWorldState)) {
			try {
				return gotBall();
			} catch (IOException e) {
				return null;
			}
		}
		
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
		Painter.target = new Vector2D[]{avoid.loc};
		Painter.lines = new Vector2D[]{new Vector2D(aiWorldState.getOwnRobot().getCoords()), avoid.loc, avoid.loc, direct.loc, direct.loc, Vector2D.add(direct.loc, Vector2D.rotateVector(new Vector2D(30, 0), direct.dir)), avoid.loc, Vector2D.add(avoid.loc, Vector2D.rotateVector(new Vector2D(30, 0), avoid.dir))};
		
		return wayPtToCommand(avoid);
	}
	
	// path executing
	private Command wayPtToCommand(final WayPt target) {
		
		final Vector2D robot = new Vector2D(aiWorldState.getOwnRobot().getCoords());
		final double robotAn = aiWorldState.getOwnRobot().getAngle();
		double dist = Vector2D.subtract(robot, target.loc).getLength();
		
		
		if (USE_OLD_STYLE_DIRECTION) {
		
		ArrayList<Waypoint> retValue = new ArrayList<Waypoint>();
		retValue.add(new Waypoint(robot, robotAn, target.loc, dist, true));
		
		
		
		return getWaypointCommand(retValue, false, DRIVING_SPEED_MULTIPLIER,
					STOP_TURN_THRESHOLD);
		
		}
		
		final double driveTime = dist / Robot.MAX_DRIVING_SPEED;
		double turnAn = robotAn - target.dir;
		final double turnTime = turnAn / Robot.MAX_TURNING_SPEED;
		
	
		// go back
		if (Math.abs(turnAn) > 90) {
			dist = -dist;
			turnAn = GeomUtils.normaliseAngle(turnAn - 180);
		}

		double turningSpeed = turnAn / driveTime;
		double drivingSpeed = dist / turnTime;
		
		if (driveTime > turnTime) {
			drivingSpeed = (dist > 0 ? 1 : -1) * Robot.MAX_DRIVING_SPEED;
		} else {
			turningSpeed = (turnAn > 0 ? 1 : -1) * Robot.MAX_TURNING_SPEED;
		}
		
		return new Command(drivingSpeed, turningSpeed, false);
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
		
		final Vector2D[] leftLines = new Vector2D[]{new Vector2D(aiWorldState.getOwnRobot().getCoords()), left.loc, left.loc, target.loc, target.loc, Vector2D.add(target.loc, Vector2D.rotateVector(new Vector2D(30, 0), target.dir))};
		final Vector2D[] rightLines =  new Vector2D[]{new Vector2D(aiWorldState.getOwnRobot().getCoords()), right.loc, right.loc, target.loc, target.loc, Vector2D.add(target.loc, Vector2D.rotateVector(new Vector2D(30, 0), target.dir))};
		
		Painter.linesSecondary = timeleft < timeright ? rightLines : leftLines;
		
		
		return timeleft < timeright ? left : right;
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
