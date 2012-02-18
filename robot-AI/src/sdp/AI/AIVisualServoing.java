package sdp.AI;

import java.io.IOException;
import java.awt.geom.Point2D;

import sdp.AI.AIWorldState.mode;
import sdp.common.Communicator;
import sdp.common.Robot;
import sdp.common.Tools;
import sdp.common.Vector2D;
import sdp.common.Communicator.opcode;

/**
 * Sends a new command every frame that overwrites the old one.
 * @author michael
 *
 */
public class AIVisualServoing extends AI {

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
		// get direction from robot to ball
		Vector2D dir = Vector2D.subtract(new Vector2D(ai_world_state.getBallCoords()), new Vector2D(ai_world_state.getRobot().getCoords()));
		// Keep the turning angle between -180 and 180
		double turning_speed = Tools.normalizeAngle(-ai_world_state.getRobot().getAngle() + Vector2D.getDirection(dir));
		// calculates speed formula:
		// speed_when_robot_next_to_ball+(distance_to_ball/max_distance)*speed_when_over_max_distance
		// every distance between 0 and max_distance will be mapped between speed_when_robot_next_to_ball and speed_when_over_max_distance
		double forward_speed = 5+(ai_world_state.getDistanceToBall()/40)*25;

		System.out.println("I'm in chase ball :), turning speed : " + turning_speed);
		// do the backwards turn
		if (turning_speed > 90 || turning_speed < -90)
			forward_speed = -20;

		// make turning faster
		//turning_angle *= 2;
		// normalize angles
		if (turning_speed > MAX_TURNING_SPEED) turning_speed = MAX_TURNING_SPEED;
		if (turning_speed < -MAX_TURNING_SPEED) turning_speed = -MAX_TURNING_SPEED;
		// don't exceed speed limit
		if (forward_speed > MAX_SPEED_CM_S) forward_speed = MAX_SPEED_CM_S;
		if (forward_speed < -MAX_SPEED_CM_S) forward_speed = -MAX_SPEED_CM_S;
		// send command
		//forward_speed/=15;
		System.out.println("forward_speed: " + forward_speed);
		//turning_angle = turning_angle > 0 ? 70 : -70;
		// make a virtual sensor at Robot.length/2 pointing at 1,0
		//double collision_dist = Tools.raytraceVector(worldState, robot, new Vector2D(Robot.LENGTH_CM/2,0), new Vector2D(1,0), am_i_blue).getLength();
		mComm.sendMessage(opcode.operate, (byte) (forward_speed), (byte) (turning_speed));
		// check whether to go into got_ball mode
		if (Math.abs(turning_speed) < TURNING_ACCURACY && ai_world_state.getDistanceToBall() < 2) {
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
		//System.out.println("Attempting to score goal");

		int can_we_shoot = ai_world_state.isGoalVisible(ai_world_state.getEnemyRobot(), ai_world_state.getEnemyGoal());

		if (can_we_shoot == -1) {
			//Something went wrong
			System.out.println("poopy");
		} else if (can_we_shoot > 0) {
			// We can see the goal
			System.out.println("We can shoot");
			Point2D.Double target = null;
			switch(can_we_shoot) {
			case 1:
				target = ai_world_state.getEnemyGoal().getTop();
				break;
			case 2:
				target = ai_world_state.getEnemyGoal().getCentre();
				break;
			case 3:
				target = ai_world_state.getEnemyGoal().getBottom();
				break;
			}

			double angle_between = anglebetween(ai_world_state.getRobot().getCoords(), target);
			double turning_angle = angle_between - ai_world_state.getRobot().getAngle();
			byte forward_speed = 2;

			// Keep the turning angle between -180 and 180
			if (turning_angle > 180) turning_angle -= 360;
			if (turning_angle < -180) turning_angle += 360;

			if (ai_world_state.getDistanceToGoal() < Robot.LENGTH_CM) forward_speed = 0;

			// don't exceed speed limit
			if (forward_speed > MAX_SPEED_CM_S) forward_speed = MAX_SPEED_CM_S;
			if (forward_speed < -MAX_SPEED_CM_S) forward_speed = -MAX_SPEED_CM_S;


			if (ai_world_state.getDistanceToBall() > Robot.LENGTH_CM) {
				ai_world_state.setMode(mode.chase_ball);
			} else if (turning_angle > TURNING_ACCURACY && (ai_world_state.getDistanceToGoal() > 1)){
				mComm.sendMessage(opcode.operate, forward_speed, (byte)20);
				//System.out.println("Going to goal - Turning: " + turning_angle);
			} 
			else if( turning_angle < -TURNING_ACCURACY && (ai_world_state.getDistanceToGoal() > 1)){
				mComm.sendMessage(opcode.operate, forward_speed, (byte)-20);
				//System.out.println("Going to goal - Turning: " + turning_angle);	
			} else  {
				mComm.sendMessage(opcode.kick);
				ai_world_state.setMode(AIWorldState.mode.chase_ball);
			}

		} else {
			// Can't see the goal

			//System.out.println("robot coords y: " + robot.getCoords().y + "   pitch_height: " + Tools.PITCH_HEIGHT_CM);
			if (ai_world_state.getRobot().getCoords().y > Tools.PITCH_HEIGHT_CM/2 + 20){
				// Robot is closest to bottom
				//System.out.println("bottom");
				mComm.sendMessage(opcode.operate, (byte) 4, (byte) -80);
			} else {
				// Robot is closest to top
				//System.out.println("top");
				mComm.sendMessage(opcode.operate, (byte) 4, (byte) 80);
			}


		}
	}
	


	/**
	 * Move behind the ball before attempting to score
	 */
	public void navigateBehindBall() {

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
