package sdp.AI;

import java.awt.geom.Point2D;
import java.io.IOException;

//import sdp.AI.neural.AINeuralNetwork;
import sdp.AI.AI.Command;
import sdp.common.Communicator;
import sdp.common.MessageListener;
import sdp.common.Communicator.opcode;
import sdp.common.WorldState;
import sdp.common.WorldStateProvider;


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

	

	private AI ai;
	private mode state = mode.SIT;
	private Communicator mComm;

	public AIMaster(Communicator comm, WorldStateProvider obs, AIMode ai_mode) {
		super(obs);
		this.mComm = comm;
			mComm.registerListener(new MessageListener() {
				@Override
				public void receiveMessage(opcode op, byte[] args, Communicator controler) {
					System.out.println(op+" "+args[0]);
					switch (op) {
					case sensor_dist:
						ai_world_state.setDist_sensor(args[0] == 1);
						break;
					case sensor_left:
						ai_world_state.setLeft_sensor(args[0] == 1);
						break;
					case sensor_right:
						ai_world_state.setRight_sensor(args[0] == 1);
						break;
					case battery:
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
			command = getCommand();
			final boolean dist_sens = ai_world_state.isDist_sensor(),
					left_sens = ai_world_state.isLeft_sensor(),
					right_sens = ai_world_state.isRight_sensor();
			
			if (left_sens) {
				command.turning_speed = 90;
			} else if (right_sens) {
				command.turning_speed = -90;
			}
			
			if (command == null){
				command = new Command(0, 0, false);
			}
			
			ai_world_state.setCommand(command);

			if (command.isDefaultAcc())
				mComm.sendMessage(opcode.operate, command.getByteSpeed(), command.getByteTurnSpeed());
			else
				mComm.sendMessage(opcode.operate, command.getByteSpeed(), command.getByteTurnSpeed(), command.getByteAcc());
			
			if (command.kick) {
				System.out.println("kicking");
				mComm.sendMessage(opcode.kick);
			}
			
			//System.out.println("ws: "+ai_world_state.getMyGoalLeft());
			
		} catch (IOException e) {
			e.printStackTrace();
		}

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
		} 

	}

	/**
	 * Change mode. Can be used for penalty, freeplay, testing, etc
	 */
	public void setState(mode new_state) {
		state = new_state;
		System.out.println("Changed State to - " + state);
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
