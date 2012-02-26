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

	private int turn_wall = 90;

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

		if ( ((ai_world_state.getRobot().getBackRight().x < ai_world_state.getEnemyGoal().getCentre().x ) 
				&& (ai_world_state.getRobot().getBackRight().x > ai_world_state.getBallCoords().x)
					&& ai_world_state.getMyGoalLeft())
						|| ((ai_world_state.getRobot().getBackRight().x > ai_world_state.getEnemyGoal().getCentre().x)
							&& (ai_world_state.getRobot().getBackRight().x < ai_world_state.getBallCoords().x))
						 		&& !ai_world_state.getMyGoalLeft()){
			System.out.println("I'm between goal and ball");
			navigateBehindBall();
		}
		
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
			int can_we_shoot = ai_world_state.isGoalVisible(ai_world_state.getEnemyRobot(), ai_world_state.getEnemyGoal());

			if (can_we_shoot > 0) {
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
				byte forward_speed = 4;

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
					//ai_world_state.setMode(AIWorldState.mode.chase_ball);
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
		
		
		while(ai_world_state.getRobot().getBackRight().x < (ai_world_state.getBallCoords().x + 15)){
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
