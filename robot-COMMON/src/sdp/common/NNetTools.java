package sdp.common;

import java.awt.geom.Point2D;

/**
 * Neural network shared tools
 * 
 * @author martinmarinov
 *
 */
public class NNetTools {
	
	public static enum move_modes {
		forward_left, forward_right,  backward_left, backward_right, forward, backward
	}
	
	public static enum got_ball_modes {
		forward_left, forward_right,  left, right, kick
	}

	/**
	 * Generates input array for the AI
	 * 
	 * @param worldState in centimeters
	 * @param am_i_blue
	 * @param my_goal_left
	 * @param id which network is receiving it
	 * @return the input array
	 */
	public static double[] generateAIinput(WorldState worldState, WorldState oldState, double dt, boolean am_i_blue, boolean my_goal_left, int id) {
		switch (id) {
		case 0:
			Vector2D ball = new Vector2D(worldState.getBallCoords());
			Vector2D ball_rel = Tools.getLocalVector(am_i_blue ? worldState.getBlueRobot() : worldState.getYellowRobot(), ball);
			double reach = Tools.reachability(worldState, ball, am_i_blue) ? 1 : -1;
			return Tools.concat(
					getSectors(worldState, am_i_blue, 5, 22),
					getTargetInSectors(ball_rel, 22),
					new double[] {
						reach
					}
					);
		case 1:
			Vector2D goal = my_goal_left ? new Vector2D(Tools.PITCH_WIDTH_CM , Tools.GOAL_Y_CM ) : new Vector2D(0 , Tools.GOAL_Y_CM );
			Vector2D goal_rel = Tools.getLocalVector(am_i_blue ? worldState.getBlueRobot() : worldState.getYellowRobot(), goal);
			return Tools.concat(
					getSectors(worldState, am_i_blue, 5, 22),
					getTargetInSectors(goal_rel, 22),
					new double[] {
						Tools.visibility(worldState, goal, am_i_blue) ? 1 : -1
					}
					);
		}
		return null;		
	}
	
	private static double[] getSectors(WorldState ws, boolean am_i_blue, int scan_count, int sector_count) {
		if (sector_count % 2 != 0 || (sector_count / 2) % 2 == 0) {
			System.out.println("Sectors must be even number which halves should be odd!");
			return null;
		}
		double[] ans = new double[sector_count];
		double sec_angle = 360d/sector_count;
		for (int i = 0; i < sector_count; i++)
			ans[i] = AI_normalizeDistanceTo1(getSector(ws, am_i_blue, Utilities.normaliseAngle(-90+i*sec_angle), Utilities.normaliseAngle(-90+(i+1)*sec_angle), scan_count), Tools.PITCH_WIDTH_CM);
		return ans;
	}
	
	private static double[] getTargetInSectors(Vector2D relative, int sector_count) {
		if (sector_count % 2 != 0 || (sector_count / 2) % 2 == 0) {
			System.out.println("Sectors must be even number which halves should be odd!");
			return null;
		}
		double[] ans = new double[sector_count];
		double sec_angle = 360d/sector_count;
		for (int i = 0; i < sector_count; i++)
			ans[i] = AI_normalizeDistanceTo1(targetInSector(relative, Utilities.normaliseAngle(-90+i*sec_angle), Utilities.normaliseAngle(-90+(i+1)*sec_angle)), Tools.PITCH_WIDTH_CM);
		return ans;
	}
	
	/**
	 * Sweeps a scanning vector sensor between the given angles and returns the distance to the closest object. Angles are wrt to the current robot
	 * where angle 0 means forward, 180 or -180 means backwards, 90 means left, -90 means right of robot. <br/>
	 * The result could be interpreted as: <i>the distance to nearest obstacle in the specified region about the current robot</i>
	 * @param ws current world state
	 * @param am_i_blue true if my robot is blue, false if it is yellow
	 * @param start_angle the starting angle of the segment (the smallest arc will be selected)
	 * @param end_angle the ending angle of the segment (the smallest arc will be selected)
	 * @param scan_count how many parts the sector should be devided for scanning
	 * @return the vector distance to the closest collision point a.k.a. the minimum distance determined by the scanning vector which swept the sector scan_count times.
	 */
	public static Vector2D getSector(WorldState ws, boolean am_i_blue, double start_angle, double end_angle, int scan_count) {
		start_angle = Utilities.normaliseAngle(start_angle);
		end_angle = Utilities.normaliseAngle(end_angle);
		final Robot me = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot();
		final Vector2D zero = Vector2D.ZERO();
		Double min_dist = null;
		Vector2D min_vec = null;
		final double sector_angle = Utilities.normaliseAngle(end_angle-start_angle);
		final double scan_angle = sector_angle/scan_count;
		for (double angle = start_angle; Utilities.normaliseAngle(end_angle-angle) * sector_angle >= 0; angle+=scan_angle) {
			final double ang_rad = angle*Math.PI/180d;
			final Vector2D distV = Tools.raytraceVector(ws, me, zero, new Vector2D(-Math.cos(ang_rad), Math.sin(ang_rad)), am_i_blue);
			final double dist = distV.getLength();
			if (min_dist == null || dist < min_dist) {
				min_dist = dist;
				min_vec = distV;
			}
		}
		return min_vec;
	}
	
	public static Vector2D targetInSector(Vector2D relative, double start_angle, double end_angle) {
		double ang = Vector2D.getDirection(relative);
		if (Utilities.normaliseAngle(end_angle - start_angle) < 0) {
			double temp = start_angle;
			start_angle = end_angle;
			end_angle = temp;
		}
		return Utilities.normaliseAngle(ang - start_angle) >= 0 && Utilities.normaliseAngle(ang - end_angle) < 0 ? relative : new Vector2D(5*Tools.PITCH_WIDTH_CM, 0);
	}
	

	public static double AI_normalizeDistanceTo1(Vector2D vec, double threshold) {
		double distance = vec.getLength();
		if (distance > threshold)
			return 1;
		return -1+2*distance/threshold;
	}
	
	/**
	 * FOR AI ONLY, DON'T USE FOR ANYTHING ELSE!
	 * Maps distance between 0 and 1
	 * @param length the length in centimeters
	 * @param threshold the normalization; if length is meaningful only for short distances, use smaller one
	 * @return mapped between 0 and 1 wrt width of pitch
	 */
	private static double AI_normalizeCoordinateTo1(double length, double threshold) {
		length = length/threshold;//(threshold-length)/threshold;
		if (length < -1)
			length = -1;
		if (length > 1)
			length = 1;
		return length;
	}

	/**
	 * FOR AI ONLY, DON'T USE FOR ANYTHING ELSE!
	 * Maps angle between 0 and 1
	 * @param angle the angle in deg
	 * @return mapped between -1 and 1 wrt width of pitch
	 */
	private static double AI_normalizeAngleTo1(double angle) {
//		angle = Utilities.normaliseAngle(angle);
//		if (Math.abs(angle) < 10)
//			return 0;
//		return angle < 0 ? -1 : 1;
		return (Utilities.normaliseAngle(angle))/180d;
	}
	
	/**
	 * Generate training output
	 * @param condition condition to be encoded
	 * @return output
	 */
	public static double[] generateOutput(int current_id, int max) {
		double[] ans  = new double[max];
		for (int i = 0; i < ans.length; i++)
			ans[i] = i == current_id ? 1 : 0;
		return ans;
	}
	
	public static double[] generateOutput(move_modes mode) {
		if (mode == null)
			return null;
		return generateOutput(mode.ordinal(), move_modes.values().length);
	}
	
	public static double[] generateOutput(got_ball_modes mode) {
		if (mode == null)
			return null;
		return generateOutput(mode.ordinal(), got_ball_modes.values().length);
	}

	/**
	 * What was the original condition
	 * 
	 * @param output array with two outputs from neural network
	 * @return the state of the original condition
	 */
	public static int recoverOutputInt(double[] output) {
		double max = 0;
		double sum = 0;
		int id = 0;
		for (int i = 0; i < output.length; i++) {
			sum+=output[i];
			if (output[i] == Double.NaN)
				return -1;
			if (output[i] > max) {
				max = output[i];
				id = i;
			}
		}
		if (sum == 0)
			return -1;
		return id;
	}
	
	public static move_modes recoverMoveOutputMode(double[] output) {
		int id = recoverOutputInt(output);
		return move_modes.values()[id];
	}
	
	public static got_ball_modes recoverGotBallOutputMode(double[] output) {
		int id = recoverOutputInt(output);
		return got_ball_modes.values()[id];
	}
	
	public static move_modes getMoveMode(int desired_speed, int desired_turning) {
		if (desired_speed > 0 && desired_turning > 0)
			return move_modes.forward_right;
		if (desired_speed > 0 && desired_turning == 0)
			return move_modes.forward;
		if (desired_speed > 0 && desired_turning < 0)
			return move_modes.forward_left;
		
		if (desired_speed == 0 && desired_turning > 0)
			return null;//move_modes.right;
		if (desired_speed == 0 && desired_turning == 0)
			return null;//move_modes.stop;
		if (desired_speed == 0 && desired_turning < 0)
			return null;//move_modes.left;

		if (desired_speed < 0 && desired_turning > 0)
			return move_modes.backward_right;
		if (desired_speed < 0 && desired_turning == 0)
			return move_modes.backward;
		if (desired_speed < 0 && desired_turning < 0)
			return move_modes.backward_left;
		
		return null;
	}
	
	public static got_ball_modes getGotBallMode(int desired_speed, int desired_turning, boolean is_kicking) {
		if (desired_speed > 0 && desired_turning > 0)
			return got_ball_modes.forward_right;
		if (desired_speed > 0 && desired_turning == 0)
			return null; //got_ball_modes.forward;
		if (desired_speed > 0 && desired_turning < 0)
			return got_ball_modes.forward_left;
		
		if (desired_speed == 0 && desired_turning > 0)
			return got_ball_modes.right;
		if (desired_speed == 0 && desired_turning == 0)
			return null; //got_ball_modes.stop;
		if (desired_speed == 0 && desired_turning < 0)
			return got_ball_modes.left;

		if (desired_speed < 0 && desired_turning > 0)
			return null; //got_ball_modes.backward_right;
		if (desired_speed < 0 && desired_turning == 0)
			return null; //got_ball_modes.backward;
		if (desired_speed < 0 && desired_turning < 0)
			return null;//got_ball_modes.backward_left;
		
		if (is_kicking)
			return got_ball_modes.kick;
		return null;
	}
	
	public static int getDesiredSpeed(move_modes mode) {
		switch (mode) {
		case forward:
		case forward_left:
		case forward_right:
			return 35;
		case backward:
		case backward_left:
		case backward_right:
			return -35;
		default:
			return 0;
		}
	}
	
	public static int getDesiredTurningSpeed(move_modes mode) {
		switch (mode) {
		case forward_right:
		case backward_right:
		//case right:
			return 90;
		case forward_left:
		case backward_left:
		//case left:
			return -90;
		default:
			return 0;
		}
	}
	
	public static int getDesiredSpeed(got_ball_modes mode) {
		switch (mode) {
		case forward_left:
		case forward_right:
			return 35;
		default:
			return 0;
		}
	}
	
	public static int getDesiredTurningSpeed(got_ball_modes mode) {
		switch (mode) {
		case forward_right:
		case right:
		//case right:
			return 90;
		case forward_left:
		case left:
		//case left:
			return -90;
		default:
			return 0;
		}
	}
	
	public static boolean getKicking(got_ball_modes mode) {
		return mode == got_ball_modes.kick;
	}
	
	/**
	 * AI:
	 * Gives the calculated probability of taking action item
	 * give network output
	 * @param item
	 * @param output
	 * @return
	 */
	public static double probability(int item, double[] output) {
		double sum = 0;
		for (int i = 0; i < output.length; i++)
			sum+=output[i];
		return output[item]/sum;
	}
	
}
