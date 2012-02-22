package sdp.AI;

import java.io.IOException;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

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
		double turning_angle = Utilities.normaliseAngle(-ai_world_state.getRobot().getAngle() + Vector2D.getDirection(dir));
			
		// calculates speed formula:
		// speed_when_robot_next_to_ball+(distance_to_ball/max_distance)*speed_when_over_max_distance
		// every distance between 0 and max_distance will be mapped between speed_when_robot_next_to_ball and speed_when_over_max_distance
		byte forward_speed = (byte) (10+(ai_world_state.getDistanceToBall()/40)*25);

		System.out.println("I'm in chase ball :), turning speed : " + turning_angle);
		// do the backwards turn
		if (turning_angle > 90 || turning_angle < -90)
			forward_speed = 0; //-20;

		turning_angle = Utilities.angleToByte(turning_angle);
		
		forward_speed = normaliseSpeed(forward_speed);
		turning_angle = Utilities.normaliseAngle(turning_angle);
		
		// make a virtual sensor at Robot.length/2 pointing at 1,0
		//double collision_dist = Tools.raytraceVector(worldState, robot, new Vector2D(Robot.LENGTH_CM/2,0), new Vector2D(1,0), am_i_blue).getLength();
		mComm.sendMessage(opcode.operate, forward_speed, (byte) (turning_angle));
		// check whether to go into got_ball mode
		
		
		//check if the ball is very close to the sides of the robot and move back 
		final Robot robot = ai_world_state.getRobot();
		
		if (Math.toDegrees(Utilities.getAngle(ai_world_state.getBallCoords(), robot.getFrontLeft(), robot.getBackLeft())) > 170
				|| Math.toDegrees(Utilities.getAngle( ai_world_state.getBallCoords(), robot.getFrontRight(),robot.getBackRight())) > 170){
			mComm.sendMessage(opcode.operate, (byte) -30, (byte) 0);
		}
		
		
		if (Math.abs(turning_angle) < TURNING_ACCURACY && ai_world_state.getDistanceToBall() < 2) {
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

		if (ai_world_state.getDistanceToBall() > Robot.LENGTH_CM) {
			ai_world_state.setMode(mode.chase_ball);
		} else {
			int can_we_shoot = ai_world_state.isGoalVisible(ai_world_state.getEnemyRobot(), ai_world_state.getEnemyGoal());

			if (can_we_shoot > 0) {
				// We can see the goal
				//System.out.println("We can shoot");
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

				if (ai_world_state.getDistanceToGoal() < Robot.LENGTH_CM) forward_speed = 0;

				// don't exceed speed limit
				forward_speed = normaliseSpeed(forward_speed);
				turning_angle = Utilities.normaliseAngle(turning_angle);

				if (turning_angle > TURNING_ACCURACY && (ai_world_state.getDistanceToGoal() > 1)){
					mComm.sendMessage(opcode.operate, forward_speed, (byte)20);
					//System.out.println("Going to goal - Turning: " + turning_angle);
				} else if( turning_angle < -TURNING_ACCURACY && (ai_world_state.getDistanceToGoal() > 1)){
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
