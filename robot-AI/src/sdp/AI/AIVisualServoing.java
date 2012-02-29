package sdp.AI;

import java.awt.geom.Point2D;
import java.io.IOException;

import sdp.AI.AIWorldState.mode;
import sdp.common.Communicator;
import sdp.common.Communicator.opcode;
import sdp.common.Robot;
import sdp.common.Tools;
import sdp.common.Utilities;
import sdp.common.Vector2D;

/**
 * Sends a new command every frame that overwrites the old one.
 * @author Michael
 *
 */
public class AIVisualServoing extends AI {
	
	/**TODO: Look at all local variables and see which ones we can make global...
	I think the agent coordinates (ball, robots) should be global as we're using them everywhere.
	-Paul.
	**/
	
	private final static int coll_secs_count = 110;
	private final static double sec_angle = 360d/coll_secs_count;
	private final static int coll_start = 25;
	private final static int corner_coll_threshold = 3;

	public AIVisualServoing(Communicator comm) {
		super(comm);
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

		Vector2D ncp = Tools.getLocalVector(ai_world_state.getRobot(),
				Vector2D.add(new Vector2D(ai_world_state.getRobot().getCoords()),
				Tools.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords())
				));
		// do the backwards turn
		
		if (ncp.getLength() < Robot.LENGTH_CM) {
			
			if (ncp.x > 0) {
				// collision in front
				if (turning_angle > 90 || turning_angle < -90) {
				//	System.out.println("in front, going backwards");
					forward_speed = -15;
				} else if (turning_angle > 30 || turning_angle < -30 ) {
				//	System.out.println("in front, turning on spot");
					forward_speed = 0;
				}
			} else {
				
				// collision behind
				if (turning_angle > 30 || turning_angle < -30 ) {
				//	System.out.println("behind on spot");
					forward_speed = 0;
				} else {
					//System.out.println("behind else");
				}
				
			}
		} else if (turning_angle > 90 || turning_angle < -90) {
			//System.out.println("no collision, going backwards");
			forward_speed = -15;
		}
		//forward_speed = 0;
		
		if(ai_world_state.getDistanceToBall() < 10 && (Math.abs(turning_angle) > 60 )){
			forward_speed = 0;
			System.out.println("Distance is under 20 and angle over 10.");
		}


		turning_angle = Utilities.normaliseAngle(turning_angle);


		forward_speed = normaliseSpeed(forward_speed);
		
		double turning_speed = Utilities.normaliseAngle(turning_angle*2);
		if (turning_speed > MAX_TURNING_SPEED) turning_speed = MAX_TURNING_SPEED;
		if (turning_speed < -MAX_TURNING_SPEED) turning_speed = -MAX_TURNING_SPEED;

		// make a virtual sensor at Robot.length/2 pointing at 1,0
		//double collision_dist = Tools.raytraceVector(worldState, robot, new Vector2D(Robot.LENGTH_CM/2,0), new Vector2D(1,0), am_i_blue).getLength();
		mComm.sendMessage(opcode.operate, forward_speed, (byte) turning_speed);		

		//check if the ball is very close to the sides of the robot and move back 
		//final Robot robot = ai_world_state.getRobot();			
		//might cause problems when very close to walls 
		
//		if (Math.toDegrees(Utilities.getAngle(ai_world_state.getBallCoords(), robot.getFrontLeft(), robot.getBackLeft())) > 170
//				|| Math.toDegrees(Utilities.getAngle( ai_world_state.getBallCoords(), robot.getFrontRight(),robot.getBackRight())) > 170){
//			mComm.sendMessage(opcode.operate, (byte) -30, (byte) 0);
//		}

		// This checks whether or not we are between enemy goal and the ball.
		// We also check whether or not the ball is too close to our goal, if it is don't try to go behind it to avoid catastrophies.
		if ( ((ai_world_state.getRobot().getCoords().x > ai_world_state.getBallCoords().x)
					&& ai_world_state.getMyGoalLeft()
						&& ai_world_state.getBallCoords().x > 10)
						|| ((ai_world_state.getRobot().getCoords().x < ai_world_state.getBallCoords().x)
						 	&& !ai_world_state.getMyGoalLeft()
								&& ai_world_state.getBallCoords().x < 230)){
			navigateBehindBall();
			
		}
				

		// check whether to go into got_ball mode
		if (Math.abs(turning_angle) < TURNING_ACCURACY && ai_world_state.getDistanceToBall() < 10) {
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
					mComm.sendMessage(opcode.operate, forward_speed, (byte)30);
					//System.out.println("Going to goal - Turning: " + turning_angle);
				} else if( turning_angle < -KICKING_ACCURACY && (ai_world_state.getDistanceToGoal() > 1)){
					mComm.sendMessage(opcode.operate, forward_speed, (byte)-30);
					//System.out.println("Going to goal - Turning: " + turning_angle);	
				} else  {
					mComm.sendMessage(opcode.kick);
					ai_world_state.setMode(AIWorldState.mode.chase_ball);
				}

			} else {
				// Can't see the goal
				if(ai_world_state.goalImage()){
					System.out.println("can see imaginary goal ");
					mComm.sendMessage(opcode.kick);
				}
				else{
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
	}

	
	/**
	 * When between the ball and the enemy goal move back towards the ball
	 * @throws IOException
	 */
	public void navigateBehindBall() throws IOException {

		Vector2D offset = new Vector2D(0,0);
		
		if(ai_world_state.getMyGoalLeft()){
			 offset = new Vector2D(-3*ai_world_state.getBallCoords().x,0);
		}
		else{
			 offset = new Vector2D(3*(Tools.PITCH_WIDTH_CM - ai_world_state.getBallCoords().x),0);
		}
		
		Vector2D ball = new Vector2D(ai_world_state.getBallCoords());
		Vector2D robotCords = new Vector2D(ai_world_state.getRobot().getCoords());
		
		Vector2D dir = Vector2D.subtract(Vector2D.add(ball, offset), robotCords);
		
		byte forward_speed = (byte) 30;
		
		double turning_angle = Utilities.normaliseAngle(-ai_world_state.getRobot().getAngle() + Vector2D.getDirection(dir));

		mComm.sendMessage(opcode.operate, forward_speed, (byte) Utilities.normaliseToByte(Utilities.normaliseAngle(turning_angle)));		

	}

	/**
	 * Defend against penalties
	 */
	public void penaltiesDefend() throws IOException {
		//TODO: Find direction of opposing robot and move into intercept path.
		//
		// Our robot will be placed like shown bellow:
		//
		// *----------------------------------------------------------*
		// |														  |
		// |														  |
		// |_														 _|
		// |  _____		 ______										  |
		// |  |	| |		|_____||									  |
		// |  |_|_| 	|_____||									  |
		// |  |___|		         									  |
		// |														  |
		// |_														 _|
		// |	     												  |
		// |                                                          |
		// *----------------------------------------------------------*
		//
		//
		// 
		//
		// Get the direction of the enemy robot and make that a vector.
		// Find the intersection between our direction vector and the enemies robot direction, this is where our robot will need to be.
		// Make our robot move to that intersection.
		//
		//
		
		Point2D.Double interceptBall= Utilities.intersection(ai_world_state.getEnemyRobot().getFrontCenter(), ai_world_state.getEnemyRobot().getCoords(), ai_world_state.getRobot().getCoords(), ai_world_state.getRobot().getFrontCenter());
		System.out.println("InterceptDistance: " + interceptBall);
		System.out.println("Our robot's y: " + ai_world_state.getRobot().getCoords().y);
		
		if (!interceptBall.equals(null)){
			
			if((interceptBall.y < ai_world_state.getMyGoal().getBottom().y)  && (interceptBall.y > ai_world_state.getMyGoal().getTop().y)){
				 if ((interceptBall.y > ai_world_state.getRobot().getCoords().y)  ){
					byte forward_speed = (byte) -20; //Utilities.normaliseToByte((15+(interceptDistance.getLength()/40)*25));
					mComm.sendMessage(opcode.operate, forward_speed, (byte) 0);
				} else if((interceptBall.y < ai_world_state.getRobot().getCoords().y)) {
					byte forward_speed = (byte) 20; //Utilities.normaliseToByte(-(15+(interceptDistance.getLength()/40)*25));
					mComm.sendMessage(opcode.operate, forward_speed, (byte) 0);
				}
			}
			else
			{
				mComm.sendMessage(opcode.operate, (byte) 0, (byte) 0);
			}
			
		}
		
	}

	/**
	 * Score a penalty
	 */
	public void penaltiesAttack() {
		//TODO: Determine shoot path - Turn and shoot quickly.
	}

	/**
	 * Block goal when in a dangerous situation
	 */
	public void protectGoal(){
		/**TODO: When the ball is in one of the corners of our goal, instead of trying to take it and risking to throw it in our own goal
				 it would be wiser to simply guard our goal and try to intercept the ball if the other robot gets it... maybe?
		**/
	}

	/**
	 * Avoid obstacles using sectors. This method is only called if ball cannot be reached directly!
	 */
	public void avoidObstacle() {

		final Vector2D ball = new Vector2D(ai_world_state.getBallCoords());
		
		// get relative ball coordinates
		final Vector2D ball_rel = Tools.getLocalVector(ai_world_state.getRobot(), ball);
		
		// get direction and distance to ball
		final double ball_dir = Vector2D.getDirection(ball_rel);
		final double ball_dist = ball_rel.getLength();
		
		// if you go directly towards the ball, at which closest point are we going to be in a collision
		final double ball_left_coll_dist = Tools.reachabilityLeft2(ai_world_state, ball, ai_world_state.getMyTeamBlue());
		final double ball_right_coll_dist = Tools.reachabilityRight2(ai_world_state, ball, ai_world_state.getMyTeamBlue());
		final double ball_coll_dist = Math.min(ball_left_coll_dist, ball_right_coll_dist);
		final double ball_vis_dist = Tools.visibility2(ai_world_state, ball, ai_world_state.getMyTeamBlue());
		
		// the angle that we need to turn in order to avoid hitting our left or right corner at the obstacle
		final double turn_ang_more = Math.toDegrees(Math.atan2(Robot.LENGTH_CM, ball_coll_dist));
		
		// get the sectors
		final double[] sectors = Tools.getSectors(ai_world_state, ai_world_state.getMyTeamBlue(), 5, coll_secs_count, false);
		
		// sectors[0] starts at at -90
		double temp = 999;
		double turning_angle = 999;
		
		// path planning if obstacle is away
		
		if (ball_vis_dist < ball_dist) {
			// if ball is not visible
			// find the sector that is closest to the ball but has a collision distance greater than the ball_coll_dist
			for (int i = 0; i < sectors.length; i++) {
				if (sectors[i] > ball_coll_dist+Robot.LENGTH_CM/2) {
					double ang = Utilities.normaliseAngle(((-90+i*sec_angle)+(-90+(i+1)*sec_angle))/2);
					double diff = Utilities.normaliseAngle(ang-ball_dir);
					if (Math.abs(diff) < Math.abs(temp)) {
						temp = diff;
						turning_angle = ang + (ball_left_coll_dist < ball_right_coll_dist ? turn_ang_more : -turn_ang_more);
					}
				}
			}

		} else {
			// if ball is visible
			turning_angle = ball_dir + (ball_left_coll_dist < ball_right_coll_dist ? turn_ang_more : -turn_ang_more);
		}
		
		// if we have no way of reaching the ball go into the most free direction
		if (turning_angle == 999) {
			temp = 0;
			for (int i = 0; i < sectors.length; i++) {
				if (sectors[i] > temp) {
					temp = sectors[i];
					double ang = Utilities.normaliseAngle(((-90+i*sec_angle)+(-90+(i+1)*sec_angle))/2);
					turning_angle = ang + (ball_left_coll_dist < ball_right_coll_dist ? turn_ang_more : -turn_ang_more);
				}
			}
		} 
		
		// set forward speed
		byte forward_speed = 0;
		if (turning_angle > 90 || turning_angle < -90)
			forward_speed = -35;
		else if (Math.abs(turning_angle) < 90)
			forward_speed = 35;


		// if we get within too close (within coll_start) of an obstacle
		double for_dist = getMin(sectors, anid(-10), anid(10)); // get collision distance at the front
		double back_dist =getMin(sectors, anid(170), anid(190)); // get collision distance at the back
		
		if (for_dist < coll_start) {
			if (forward_speed >= 0) {
				// go backwards
				double speed_coeff = -1+for_dist/coll_start;
				if (speed_coeff > 0)
					speed_coeff = 0;
				if (speed_coeff < -1)
					speed_coeff = -1;
				forward_speed *= speed_coeff;
			}
		} else if (back_dist < coll_start) {
			if (forward_speed <= 0) {
				// same as above
				double speed_coeff = -1+back_dist/coll_start;
				if (speed_coeff > 0)
					speed_coeff = 0;
				if (speed_coeff < -1)
					speed_coeff = -1;
				forward_speed *= speed_coeff;
			}
		}
		
		// check if either of the edges are in collision
		final Vector2D
				front_left = Tools.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getBackLeft()),Tools.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
				front_right = Tools.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getBackRight()),Tools.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
				back_left = Tools.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getFrontLeft()),Tools.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
				back_right = Tools.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getFrontRight()),Tools.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords())));
		final boolean
				front_left_coll = front_left.getLength() <= corner_coll_threshold,
				front_right_coll = front_right.getLength() <= corner_coll_threshold,
				back_left_coll = back_left.getLength() <= corner_coll_threshold,
				back_right_coll = back_right.getLength() <= corner_coll_threshold,
				any_collision = front_left_coll || front_right_coll || back_left_coll || back_right_coll;
		
		if (any_collision) {
			//System.out.println("Collision "+(front_left_coll || front_right_coll ? "FRONT" : "BACK")+" "+(front_left_coll || back_left_coll ? "LEFT" : "RIGHT"));
			forward_speed = (byte) (front_left_coll || front_right_coll ? -35 : 35);
			turning_angle += front_left_coll || back_left_coll ? -10 : 10;
		}
		
		// acclerate all turning
		turning_angle *= 2;
		
		// normalize
		forward_speed = normaliseSpeed(forward_speed);
		turning_angle =  Utilities.normaliseToByte(Utilities.normaliseAngle(turning_angle));
		
		// send command
		try {
			mComm.sendMessage(opcode.operate, forward_speed, (byte) (turning_angle));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Play ball from walls
//	 */
//	public void kickWithWalls() throws IOException{
//		boolean shootWithWall = ai_world_state.goalImage();
//		System.out.println("can see imaginary goal " + shootWithWall);
//		mComm.sendMessage(opcode.kick);
//	}
	
	/**
	 * Gives the ID of a given angle. Angle is wrt to (1, 0) local vector on robot (i.e. 0 is forward)
	 * @param angle should not exceed
	 * @return
	 */
	private int anid(double angle) {
		int id =  (int) (coll_secs_count*(angle+90)/360);
		return id < 0 ? coll_secs_count + id : (id >= coll_secs_count ? id - coll_secs_count : id);
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
}
