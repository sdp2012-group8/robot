package sdp.common;

import sdp.common.geometry.Vector2D;

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
			Vector2D ball_rel = Utilities.getLocalVector(am_i_blue ? worldState.getBlueRobot() : worldState.getYellowRobot(), ball);
			double reach = Utilities.reachability(worldState, ball, am_i_blue, false, Robot.LENGTH_CM) ? 1 : -1;
			return Utilities.concat(
					Utilities.getSectors(worldState, am_i_blue, 5, 22, true, false),
					Utilities.getTargetInSectors(ball_rel, 22),
					new double[] {
						reach
					}
					);
		case 1:

			Vector2D goal = my_goal_left ? new Vector2D(WorldState.PITCH_WIDTH_CM , WorldState.GOAL_HEIGHT_CM ) : new Vector2D(0 , WorldState.GOAL_HEIGHT_CM );
			Vector2D goal_rel = Utilities.getLocalVector(am_i_blue ? worldState.getBlueRobot() : worldState.getYellowRobot(), goal);
			return Utilities.concat(
					Utilities.getSectors(worldState, am_i_blue, 5, 22, true, false),
					Utilities.getTargetInSectors(goal_rel, 22),
					new double[] {
						Utilities.visibility(worldState, goal, am_i_blue, false) ? 1 : -1
					}
					);
		}
		return null;		
	}
	
	public static Vector2D targetInSector(Vector2D relative, double start_angle, double end_angle) {
		double ang = Vector2D.getDirection(relative);
		if (Utilities.normaliseAngleToDegrees(end_angle - start_angle) < 0) {
			double temp = start_angle;
			start_angle = end_angle;
			end_angle = temp;
		}

		return Utilities.normaliseAngleToDegrees(ang - start_angle) >= 0 && Utilities.normaliseAngleToDegrees(ang - end_angle) < 0 ? relative : new Vector2D(5*WorldState.PITCH_WIDTH_CM, 0);
	}
	

	public static double AI_normalizeDistanceTo1(Vector2D vec, double threshold) {
		double distance = vec.getLength();
		if (distance > threshold)
			return 1;
		return -1+2*distance/threshold;
	}
	
//	/**
//	 * FOR AI ONLY, DON'T USE FOR ANYTHING ELSE!
//	 * Maps distance between 0 and 1
//	 * @param length the length in centimeters
//	 * @param threshold the normalization; if length is meaningful only for short distances, use smaller one
//	 * @return mapped between 0 and 1 wrt width of pitch
//	 */
//	private static double AI_normalizeCoordinateTo1(double length, double threshold) {
//		length = length/threshold;//(threshold-length)/threshold;
//		if (length < -1)
//			length = -1;
//		if (length > 1)
//			length = 1;
//		return length;
//	}
//
//	/**
//	 * FOR AI ONLY, DON'T USE FOR ANYTHING ELSE!
//	 * Maps angle between 0 and 1
//	 * @param angle the angle in deg
//	 * @return mapped between -1 and 1 wrt width of pitch
//	 */
//	private static double AI_normalizeAngleTo1(double angle) {
////		angle = Utilities.normaliseAngle(angle);
////		if (Math.abs(angle) < 10)
////			return 0;
////		return angle < 0 ? -1 : 1;
//		return (Utilities.normaliseAngle(angle))/180d;
//	}
	
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
