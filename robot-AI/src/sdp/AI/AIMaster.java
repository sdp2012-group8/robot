package sdp.AI;

import java.io.IOException;

import sdp.AI.AIWorldState.mode;
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
	
	public enum AIMode {
		visual_servoing, neural_network
	}
	
	private AI ai;

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
		ai.update(ai_world_state);
		try {
			switch (ai_world_state.getMode()) {
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
	
	public void close() {
		ai.close();
	}
	
	/**
	 * Used to set the mode of the AI.
	 * @param new_mode
	 */
	public void setMode(AIWorldState.mode new_mode) {
		ai_world_state.setState(new_mode);
	}
	
	/**
	 * Used to get the current AI mode.
	 * @return
	 */
	public AIWorldState.mode getMode() {
		return ai_world_state.getMode();
	}

}
