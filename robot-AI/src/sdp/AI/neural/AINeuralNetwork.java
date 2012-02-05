package sdp.AI.neural;

import java.io.IOException;

import org.neuroph.core.NeuralNetwork;

import sdp.AI.AI;
import sdp.common.Communicator;
import sdp.common.Tools;
import sdp.common.WorldStateProvider;
import sdp.common.Communicator.opcode;

public class AINeuralNetwork extends AI {

	private NeuralNetwork neuralNetwork;
	private boolean blue_selected;

	public AINeuralNetwork(Communicator Comm, WorldStateProvider Obs, String fname, boolean blue_selected) {
		super(Comm, Obs);
		this.blue_selected = blue_selected;
		neuralNetwork = NeuralNetwork.load(fname);
	}

	@Override
	protected void worldChanged() {
		switch (state) {
		case chase_ball:
			chaseBall();
			break;
		}

	}
	
	private void chaseBall() {
		
		neuralNetwork.setInput(Tools.generateAIinput(worldState, blue_selected, my_goal_left));
		neuralNetwork.calculate();
		double[] output = neuralNetwork.getOutput();
		boolean is_going_forwards	= output[0] > 0.5,
				is_standing_still 	= output[1] > 0.5,
				is_turning_right 	= output[2] > 0.5,
				is_not_turning 		= output[3] > 0.5,
				is_it_kicking 		= output[4] > 0.5;
		System.out.println("forwards "+is_going_forwards+"("+output[0]+"); " +
				"still "+is_standing_still+"("+output[1]+"); " +
						"right "+is_turning_right+"("+output[2]+"); " +
								"not_turn "+is_not_turning+"("+output[3]+"); " +
										"kick "+is_it_kicking+"("+output[4]+"); ");
		try {
			int speed = is_standing_still ? 0 : (is_going_forwards ? MAX_SPEED_CM_S : - MAX_SPEED_CM_S);
			int turn_speed =  is_not_turning ? 0 : (is_turning_right ? 127 : -127); 
			mComm.sendMessage(opcode.operate, (byte) speed, (byte) turn_speed);
			if (is_it_kicking)
				mComm.sendMessage(opcode.kick);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
