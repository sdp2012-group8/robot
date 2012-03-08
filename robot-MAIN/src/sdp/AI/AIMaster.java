package sdp.AI;

import java.io.IOException;

//import sdp.AI.neural.AINeuralNetwork;
import sdp.AI.AI.Command;
import sdp.common.Communicator;
import sdp.common.MessageListener;
import sdp.common.Communicator.opcode;
import sdp.common.WorldStateProvider;


/**
 * This class is the controller class for all the AI's.
 * Any other system that needs to use an AI should use this.
 * @author michael
 *
 */
public class AIMaster extends AIListener {

	public enum mode {
		chase_ball, got_ball, defend_goal, sit, defend_penalties, attack_penalties
	}

	public enum AIMode {
		visual_servoing, neural_network
	}

	public static final int DIST_TO_BALL = 6;

	private AI ai;
	private mode state = mode.sit;
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

		switch(ai_mode) {
		case visual_servoing:
			ai = new AIVisualServoing();
			break;
		case neural_network:
			//ai = new AINeuralNetwork(comm, "data");
			break;
		}
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

			if (command.isDefaultAcc())
				mComm.sendMessage(opcode.operate, command.getByteSpeed(), command.getByteTurnSpeed());
			else
				mComm.sendMessage(opcode.operate, command.getByteSpeed(), command.getByteTurnSpeed(), command.getByteAcc());
			
			if (command.kick) mComm.sendMessage(opcode.kick);
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private AI.Command getCommand() throws IOException {
		switch (getState()) {
		case chase_ball:
			return ai.chaseBall();
		case got_ball:
			//setState(mode.sit);
			return ai.gotBall();
		case sit:
			return ai.sit();
		case defend_penalties:
			return ai.penaltiesDefend();
		case attack_penalties:
			return ai.penaltiesAttack();
		case defend_goal:
			return ai.defendGoal();
		default:
			return null;
		}
	}

	private void checkState() {
		// Check the new world state and decide what state we should be in.

		// Can now change between states more easily
//		if (getState() == mode.chase_ball && ai_world_state.getDistanceToBall() > DIST_TO_BALL){
//			setState(mode.chase_ball);
//		} else if (getState() == mode.defend_penalties){
//			setState(mode.defend_penalties);
//		} else if(ai_world_state.getDistanceToBall() < DIST_TO_BALL){
//			setState(mode.got_ball);
//		} else if(getState()== mode.sit){
//			setState(mode.sit);
//		}


		if (state != mode.defend_goal) {
			if (getState() != mode.sit) {
				if (ai_world_state.getDistanceToBall() > DIST_TO_BALL) {
					setState(mode.chase_ball);
				} else {
					setState(mode.got_ball);
				}		
			}

		}
	}

	/**
	 * Change mode. Can be used for penalty, freeplay, testing, etc
	 */
	public void setState(mode new_state) {
		state = new_state;
	}

	/**
	 * Gets AI mode
	 * @return
	 */
	public mode getState() {
		return state;
	}

}