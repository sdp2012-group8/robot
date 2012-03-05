package sdp.AI;

import java.io.IOException;

//import sdp.AI.neural.AINeuralNetwork;
import sdp.AI.AI.Command;
import sdp.common.Communicator;
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
	
	public static final int DIST_TO_BALL = 10;
	
	private AI ai;
	private mode state = mode.sit;
	private Communicator mComm;

	public AIMaster(Communicator comm, WorldStateProvider obs, AIMode ai_mode) {
		super(obs);
		this.mComm = comm;
		
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
			if (command == null)
				command = new Command(0, 0, false);
			if (command.isDefaultAcc())
				mComm.sendMessage(opcode.operate, command.getByteSpeed(), command.getByteTurnSpeed());
			else
				mComm.sendMessage(opcode.operate, command.getByteSpeed(), command.getByteTurnSpeed());//, command.getByteAcc());
			if (command.getByteSpeed() != 0 && command.getByteTurnSpeed() != 0)
				System.out.println(command);
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
			setState(mode.sit);
			return ai.gotBall();
		case sit:
			return ai.sit();
		case defend_penalties:
			return ai.penaltiesDefend();
		case attack_penalties:
			return ai.penaltiesAttack();
		default:
			return null;
		}
	}
	
	private void checkState() {
		// Check the new world state and decide what state we should be in.
		if (ai_world_state.getDistanceToBall() > DIST_TO_BALL && getState() != mode.sit) {
			setState(mode.chase_ball);
		} else {
			setState(mode.got_ball);
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
