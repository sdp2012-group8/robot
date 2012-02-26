package sdp.AI;

import java.io.IOException;
import sdp.AI.AIWorldState.mode;
import sdp.common.Communicator;
import sdp.common.Robot;
import sdp.common.Tools;
import sdp.common.Utilities;
import sdp.common.Vector2D;
import sdp.common.Communicator.opcode;

/**
 * Sends a new command every frame that overwrites the old one.
 * @author Michael
 *
 */
public class AIVisualServoing extends AI {
	
	private final static int coll_secs_count = 22;
	private final static double sec_angle = 360d/coll_secs_count;
	private final static int coll_start = 30;

	public AIVisualServoing(Communicator comm) {
		super(comm);
	}


	public void avoidObstacle() {
		Vector2D ball_rel = Tools.getLocalVector(ai_world_state.getRobot(), new Vector2D(ai_world_state.getBallCoords()));
		double ball_dir = Vector2D.getDirection(ball_rel);
		double ball_dist = ball_rel.getLength();
		double[] sectors = Tools.getSectors(ai_world_state, ai_world_state.getMyTeamBlue(), 5, coll_secs_count, false);
		// sectors[0] starts at at -90
		double min_diff = 999;
		double turning_angle = 999;
		double dist = 0;
		for (int i = 0; i < sectors.length; i++) {
			if (sectors[i] > ball_dist) {
				double ang = ((-90+i*sec_angle)+(-90+(i+1)*sec_angle))/2;
				double diff = Utilities.normaliseAngle(ang-ball_dir);
				if (Math.abs(diff) < Math.abs(min_diff)) {
					min_diff = diff;
					turning_angle = Utilities.normaliseAngle(ang);
					dist = sectors[i];
				}
			}
		}
		
		if (turning_angle == 999) {
			double max = 0;
			for (int i = 0; i < sectors.length; i++) {
					if (sectors[i] > max) {
						max = sectors[i];
						double ang = ((-90+i*sec_angle)+(-90+(i+1)*sec_angle))/2;
						turning_angle = Utilities.normaliseAngle(ang);
						dist = sectors[i];
					}
			}
		}
		
		double turn_ang_more = Math.toDegrees(Math.atan2(Robot.LENGTH/2, dist));
		
		turning_angle += turning_angle > 0 ? turn_ang_more : -turn_ang_more;
		
		byte forward_speed = 0;
		if (turning_angle > 90 || turning_angle < -90)
			forward_speed = -35;
		else if (Math.abs(turning_angle) < 90)
			forward_speed = 35;

		
		double for_dist = sectors[coll_secs_count/4];//getMin(sectors, coll_secs_count/8, 3*coll_secs_count/8);//sectors[coll_secs_count/4];
		double back_dist = sectors[3*coll_secs_count/4];//getMin(sectors, 5*coll_secs_count/8, 7*coll_secs_count/8);//sectors[3*coll_secs_count/4];
		if (for_dist < coll_start) {
			if (forward_speed >= 0) {
				double speed_coeff = -1+for_dist/coll_start;
				if (speed_coeff > 0)
					speed_coeff = 0;
				if (speed_coeff < -1)
					speed_coeff = -1;
				forward_speed *= speed_coeff;
			}
		} else if (back_dist < coll_start) {
			if (forward_speed <= 0) {
				double speed_coeff = -1+back_dist/coll_start;
				if (speed_coeff > 0)
					speed_coeff = 0;
				if (speed_coeff < -1)
					speed_coeff = -1;
				forward_speed *= speed_coeff;
			}
		}
		
		//System.out.println("Forward collision "+sectors[coll_secs_count/4 + 1]+" back collision "+sectors[3*coll_secs_count/4 + 1]);
		
		
		turning_angle = Utilities.normaliseToByte(turning_angle);
		
		//System.out.println("Turning angle "+turning_angle); 

		forward_speed = normaliseSpeed(forward_speed);
		turning_angle = Utilities.normaliseAngle(turning_angle);

		// make a virtual sensor at Robot.length/2 pointing at 1,0
		//double collision_dist = Tools.raytraceVector(worldState, robot, new Vector2D(Robot.LENGTH_CM/2,0), new Vector2D(1,0), am_i_blue).getLength();
		try {
			mComm.sendMessage(opcode.operate, forward_speed, (byte) (turning_angle));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Find the smallest element in the array from start to end inclusive
	 * 
	 * @param array
	 * @param start
	 * @param end
	 * @return
	 */
	private static final double getMin(double[] array, int start, int end) {
		double min = 9999;
		for (int i = start; i <= end; i++)
			if (array[i] < min)
				min = array[i];
		return min;
	}
	
	/**
	 * Mode to go towards the ball.
	 * When it reaches the ball, transition into the goToGoal state.
	 * WARNING - Doesn't use and prediction to go towards the ball.
	 * @throws IOException 
	 */
	public void chaseBall() throws IOException {
		if (!Tools.reachability(ai_world_state, new Vector2D(ai_world_state.getBallCoords()), ai_world_state.getMyTeamBlue())) {
			// if ball is not directly reachable
			avoidObstacle();
			return;
		}
		// get direction from robot to ball
		Vector2D dir = Vector2D.subtract(new Vector2D(ai_world_state.getBallCoords()), new Vector2D(ai_world_state.getRobot().getCoords()));
		// Keep the turning angle between -180 and 180
		double turning_angle = Utilities.normaliseAngle(-ai_world_state.getRobot().getAngle() + Vector2D.getDirection(dir));

		// calculates speed formula:
		// speed_when_robot_next_to_ball+(distance_to_ball/max_distance)*speed_when_over_max_distance
		// every distance between 0 and max_distance will be mapped between speed_when_robot_next_to_ball and speed_when_over_max_distance
		byte forward_speed = (byte) Utilities.normaliseToByte((15+(ai_world_state.getDistanceToBall()/40)*25));

		//System.out.println("I'm in chase ball :), turning speed : " + turning_angle);
		Vector2D ncp = Tools.getLocalVector(ai_world_state.getRobot(),
				Vector2D.add(new Vector2D(ai_world_state.getRobot().getCoords()),
				Tools.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords())
				));
		// do the backwards turn
		if (ncp.getLength() < Robot.LENGTH_CM) {
			//System.out.println("collision "+ncp.x+" and "+ncp.y+" dist "+ncp.getLength());
			if (ncp.x > 0) {
				// collision in front
				if (turning_angle > 90 || turning_angle < -90) {
					System.out.println("in front, going backwards");
					forward_speed = -15;
				} else if (turning_angle > 30 || turning_angle < -30) {
					System.out.println("in front, turning on spot");
					forward_speed = 0;
				}
			} else {
				
				// collision behind
				if (turning_angle > 30 || turning_angle < -30) {
					System.out.println("behind on spot");
					forward_speed = 0;
				} else
				System.out.println("behind else");
			}
		} else if (turning_angle > 90 || turning_angle < -90) {
			System.out.println("no collision, going backwards");
			forward_speed = -15;
		}
		
		//forward_speed = 0;




		turning_angle = Utilities.normaliseToByte(turning_angle);


		forward_speed = normaliseSpeed(forward_speed);
		turning_angle = Utilities.normaliseAngle(turning_angle);

		// make a virtual sensor at Robot.length/2 pointing at 1,0
		//double collision_dist = Tools.raytraceVector(worldState, robot, new Vector2D(Robot.LENGTH_CM/2,0), new Vector2D(1,0), am_i_blue).getLength();
		mComm.sendMessage(opcode.operate, forward_speed, (byte) (turning_angle));		

		//check if the ball is very close to the sides of the robot and move back 
		final Robot robot = ai_world_state.getRobot();			
		//might cause problems when very close to walls 
		if (Math.toDegrees(Utilities.getAngle(ai_world_state.getBallCoords(), robot.getFrontLeft(), robot.getBackLeft())) > 170
				|| Math.toDegrees(Utilities.getAngle( ai_world_state.getBallCoords(), robot.getFrontRight(),robot.getBackRight())) > 170){
			mComm.sendMessage(opcode.operate, (byte) -30, (byte) 0);
		}

//		if ( ((ai_world_state.getRobot().getBackRight().x < ai_world_state.getEnemyGoal().getCentre().x ) 
//				&& (ai_world_state.getRobot().getBackRight().x > ai_world_state.getBallCoords().x)
//					&& ai_world_state.getMyGoalLeft())
//						|| ((ai_world_state.getRobot().getBackRight().x > ai_world_state.getEnemyGoal().getCentre().x)
//							&& (ai_world_state.getRobot().getBackRight().x < ai_world_state.getBallCoords().x))
//						 		&& !ai_world_state.getMyGoalLeft()){
//			System.out.println("I'm between goal and ball");
//			navigateBehindBall();
//		}
		
		// check whether to go into got_ball mode
		if (Math.abs(turning_angle) < TURNING_ACCURACY && ai_world_state.getDistanceToBall() < 10) {
			//ai_world_state.setMode(mode.sit);
			ai_world_state.setMode(mode.got_ball);
		}
		
	}

	/**
	 * Mode set if got ball.
	 * Aims and shoots the ball into the opposing goal. 
	 * @throws IOException 
	 */
	public void gotBall() throws IOException{
		System.out.println("Attempting to score goal");

		System.out.println("Distance to ball: " + ai_world_state.getDistanceToBall());

		if (ai_world_state.getDistanceToBall() > 10) {
			ai_world_state.setMode(mode.chase_ball);
		} else {
			boolean can_we_shoot = ai_world_state.isGoalVisible();

			if (can_we_shoot) {
				// We can see the goal
				System.out.println("We can shoot");

				double angle_between = ai_world_state.calculateShootAngle();
				double turning_angle = angle_between - ai_world_state.getRobot().getAngle();
				byte forward_speed = 4;

				if (ai_world_state.getDistanceToGoal() < Robot.LENGTH_CM) forward_speed = 0;

				// don't exceed speed limit
				forward_speed = normaliseSpeed(forward_speed);
				turning_angle = Utilities.normaliseAngle(turning_angle);

				if (turning_angle > KICKING_ACCURACY && (ai_world_state.getDistanceToGoal() > 1)){
					mComm.sendMessage(opcode.operate, forward_speed, (byte)20);
					//System.out.println("Going to goal - Turning: " + turning_angle);
				} else if( turning_angle < -KICKING_ACCURACY && (ai_world_state.getDistanceToGoal() > 1)){
					mComm.sendMessage(opcode.operate, forward_speed, (byte)-20);
					//System.out.println("Going to goal - Turning: " + turning_angle);	
				} else  {
					mComm.sendMessage(opcode.kick);
					ai_world_state.setMode(AIWorldState.mode.chase_ball);
				}

			} else {
				// Can't see the goal

				if (ai_world_state.getRobot().getCoords().y > Tools.PITCH_HEIGHT_CM/2 + 20){
					// Robot is closest to bottom
					mComm.sendMessage(opcode.operate, (byte) 10, (byte) -80);
				} else {
					// Robot is closest to top
					mComm.sendMessage(opcode.operate, (byte) 10, (byte) 80);
				}

			}
		}
	}

	/**
	 * Move behind the ball before attempting to score
	 */
	public void navigateBehindBall() throws IOException {
		//TODO: Whenever we are between the enemy goal and the ball, get the ball and re-orientate ourselves towards the enemy goal
		System.out.println("In navigateBehindBall");
		
		Vector2D dir = Vector2D.subtract(new Vector2D(ai_world_state.getBallCoords()), new Vector2D(ai_world_state.getRobot().getCoords()));
		byte forward_speed = (byte) Utilities.normaliseToByte((15+(ai_world_state.getDistanceToBall()/40)*25));
		double turning_angle = Utilities.normaliseAngle(-ai_world_state.getRobot().getAngle() + Vector2D.getDirection(dir));
		
		
		if (ai_world_state.getRobot().getBackRight().x < (ai_world_state.getBallCoords().x )){
			mComm.sendMessage(opcode.operate, forward_speed, (byte) Utilities.normaliseToByte(Utilities.normaliseAngle(turning_angle)));
		}
		
		ai_world_state.setMode(mode.chase_ball);
	}

	/**
	 * Defend against penalties
	 */
	public void penaltiesDefend() {
		//TODO: Find direction of opposing robot and move into intercept path.
	}

	/**
	 * Score a penalty
	 */
	public void penaltiesAttack() {
		//TODO: Determine shoot path - Turn and shoot quickly.
	}

}
