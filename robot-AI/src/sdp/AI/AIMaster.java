package sdp.AI;

import java.io.IOException;

import javax.jws.WebParam.Mode;

import sdp.AI.neural.AINeuralNetwork;
import sdp.common.Communicator;
import sdp.common.WorldStateProvider;


/**
 * This class is the controller class for all the AI's.
 * Any other system that needs to use an AI should use this.
 * @author michael
 *
 */
public class AIMaster extends AIListener {
	
	public enum mode {
		chase_ball, sit, got_ball, dribble, defend_penalties, attack_penalties
	}
	
	public enum AIMode {
		visual_servoing, neural_network
	}
	
	private AI ai;
	private mode state = mode.sit;

	public AIMaster(Communicator comm, WorldStateProvider obs, AIMode ai_mode) {
		super(obs);
		
		switch(ai_mode) {
		case visual_servoing:
			ai = new AIVisualServoing(comm);
			break;
		case neural_network:
			ai = new AINeuralNetwork(comm, "data");
			break;
		}
	}

	/**
	 * This method is fired when a new state is available.
	 * The methods called are in all types of the AI.
	 */
	protected synchronized void worldChanged() {
		checkState();
		ai.update(ai_world_state);
		try {
			switch (getState()) {
			case chase_ball:
				ai.chaseBall();
				break;
			case got_ball:
				ai.gotBall();
				break;
			case sit:
				//if (ai.old_ai_world_state == null || ai.old_ai_world_state.getMode() != mode.sit) {
					ai.sit();
				//}
				break;
			case defend_penalties:
					ai.penaltiesDefend();
					break;
			case attack_penalties:
					ai.penaltiesAttack();
					break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void checkState() {
		// Check the new world state and decide what state we should be in.
		
		if (ai_world_state.getDistanceToBall() > 10) {
			setState(mode.chase_ball);
		} else {
			setState(mode.sit);
		}		
		
	}

	public void close() {
		ai.close();
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
