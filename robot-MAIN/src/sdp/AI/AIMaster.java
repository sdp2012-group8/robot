package sdp.AI;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

//import sdp.AI.neural.AINeuralNetwork;
import sdp.AI.AI.Command;
import sdp.common.Communicator;
import sdp.common.MessageListener;
import sdp.common.Communicator.opcode;
import sdp.common.WorldState;
import sdp.common.WorldStateProvider;
import sdp.common.geometry.Vector2D;


/**
 * This class is the controller class for all the AI's.
 * Any other system that needs to use an AI should use this.
 * @author michael
 *
 */
public class AIMaster extends AIListener {

	// Eclipse shouted at me saying they shouldn't be lowercase.
	/** The mode the AI is in.
	 * This should be states common to all AI's.
	 */
	public enum mode {
		PLAY, DEFEND_GOAL, SIT, DEFEND_PENALTIES, SHOOT_PENALTIES, MANUAL_CONTROL
	}

	/** The type of AI that is running */
	public enum AIMode {
		VISUAL_SERVOING, NEURAL_NETWORKS
	}
	
	/** Distance from the goal when the AI changes from Penalty mode to play mode */
	private final static int PENALTIES_THRESH = 30; 

	/** The threshold distance for defend goal */
	private final static double DEFEND_THRESH = WorldState.PITCH_WIDTH_CM / 2 - 20;

	private AI ai;
	private mode state = mode.SIT;
	private Communicator mComm;

	public AIMaster(Communicator comm, WorldStateProvider obs, AIMode ai_mode) {
		super(obs);
		this.mComm = comm;
			mComm.registerListener(new MessageListener() {
				@Override
				public void receiveMessage(opcode op, short[] args, Communicator controler) {
					//System.out.println(op+" "+args[0]);
					switch (op) {
					case SENSOR_KICKER:
//						try {
//							mComm.sendMessage(opcode.kick);
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
						ai_world_state.setDist_sensor(args[0] == 1);
						try {
							execCommand(ai.gotBall());
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						break;
					case SENSOR_LEFT:
						ai_world_state.setLeft_sensor(args[0] == 1);
						break;
					case SENSOR_RIGHT:
						ai_world_state.setRight_sensor(args[0] == 1);
						break;
					case BATTERY:
						ai_world_state.setBattery(args[0]);
						break;
					}

				}
			});

		setAIMode(ai_mode);
	}

	/**
	 * This method is fired when a new state is available.
	 * The methods called are in all types of the AI.
	 */
	protected synchronized void worldChanged() {
		
		AI.Command command;

		checkState();
		ai.update(ai_world_state);
		
		try {
			execCommand(getCommand());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void execCommand(Command command) throws IOException {
			final boolean dist_sens = ai_world_state.isDist_sensor(),
					left_sens = ai_world_state.isLeft_sensor(),
					right_sens = ai_world_state.isRight_sensor();
			
			if (left_sens) {
				command.turning_speed = 90;
			} else if (right_sens) {
				command.turning_speed = -90;
			}
			
			if (command != null){
				ai_world_state.setCommand(command);
			
			
	//		ai_world_state.setCommand(command);

			if (command.isDefaultAcc())
				mComm.sendMessage(opcode.operate, command.getShortSpeed(), command.getShortTurnSpeed());
			else
				mComm.sendMessage(opcode.operate, command.getShortSpeed(), command.getShortTurnSpeed(), command.getShortAcc());
			
			if (command.kick) {
				System.out.println("kicking");
				mComm.sendMessage(opcode.kick);
			}
			}
			//System.out.println("ws: "+ai_world_state.getMyGoalLeft());
	}

	private AI.Command getCommand() throws IOException {
		switch (getState()) {
		case PLAY:
			return ai.chaseBall();
		case SIT:
			return ai.sit();
		case DEFEND_PENALTIES:
			return ai.penaltiesDefend();
		case SHOOT_PENALTIES:
			return ai.penaltiesAttack();
		case DEFEND_GOAL:
			return ai.defendGoal();
		default:
			return null;
		}
	}

	private void checkState() {
		// Check the new world state and decide what state we should be in.

		final boolean my_goal_left = ai_world_state.getMyGoalLeft();
		final Point2D.Double ball = ai_world_state.getBallCoords();
		
		// DEFEND_PENALTIES -> PLAY
		if (state == mode.DEFEND_PENALTIES) {
			if (my_goal_left && ball.x < PENALTIES_THRESH ||
					!my_goal_left && ball.x > WorldState.PITCH_WIDTH_CM - PENALTIES_THRESH) {
				setState(mode.PLAY);
			}
		} else // SHOOT_PENALTIES -> PLAY
			if (state == mode.SHOOT_PENALTIES) {
				if (!my_goal_left && ball.x < PENALTIES_THRESH ||
						my_goal_left && ball.x > WorldState.PITCH_WIDTH_CM - PENALTIES_THRESH) {
					setState(mode.PLAY);
				}
		} else  //PLAY->DEFEND_GOAL
			if (state == mode.PLAY) {
				//if the enemy robot has the ball and it is close to our goal, go to defend
//				Vector2D enemyDistance = Vector2D.subtract(new Vector2D(ai_world_state.getEnemyRobot().getCoords()), new Vector2D(ball));
//				
//				if ((((my_goal_left && ball.x < DEFEND_THRESH)  || 
//						(!my_goal_left && ball.x < WorldState.PITCH_WIDTH_CM - DEFEND_THRESH))
//						&& enemyDistance.getLength() < 30)){
//					setState(mode.DEFEND_GOAL);
//				}
						
			}
			else  //DEFEND_GOAL -> PLAY 
				if (state == mode.DEFEND_GOAL) {
					//if the enemy robot is at a greater distance from the ball, go into play mode
//					Vector2D enemyDistance = Vector2D.subtract(new Vector2D(ai_world_state.getEnemyRobot().getCoords()), new Vector2D(ball));
//					
//					if (enemyDistance.getLength() > 30){
//						setState(mode.PLAY);
//					}
					
					Vector2D myDistance = Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), new Vector2D(ball));
					
					if (myDistance.getLength() < 20)
						setState(mode.PLAY);
				}

	}

	/**
	 * Change mode. Can be used for penalty, freeplay, testing, etc
	 */
	public void setState(mode new_state) {
		state = new_state;
		System.out.println("Changed State to - " + state);

		
//		if (state == mode.DEFEND_GOAL) {
//			Timer t = new Timer();
//			t.schedule(new TimerTask() {
//				@Override
//				public void run() {
//					setState(AIMaster.mode.PLAY);
//				}
//			}, 6000);
//		}

		ai.changedState();

	}
	
	/**
	 * Change the AI mode. Can be used in the simulator to test separate modes
	 * @param new_ai
	 */
	public void setAIMode(AIMode new_ai_mode){
		switch (new_ai_mode){
			case VISUAL_SERVOING:
				ai = new AIVisualServoing();
				break;
			case NEURAL_NETWORKS: 
				ai = new AITest();
				break;
		}
	}
	
	/**
	 * Gets AI mode
	 * @return
	 */
	public mode getState() {
		return state;
	}

}
