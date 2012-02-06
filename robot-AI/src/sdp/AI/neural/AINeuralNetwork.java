package sdp.AI.neural;

import java.io.IOException;

import org.neuroph.core.NeuralNetwork;

import sdp.AI.AI;
import sdp.common.Communicator;
import sdp.common.Tools;
import sdp.common.WorldStateProvider;
import sdp.common.Communicator.opcode;

public class AINeuralNetwork extends AI {
	
	private final static int network_count = 3;
	private NeuralNetwork[] nets = new NeuralNetwork[network_count];
	private boolean blue_selected;

	public AINeuralNetwork(Communicator Comm, WorldStateProvider Obs, String fname, boolean blue_selected) {
		super(Comm, Obs);
		this.blue_selected = blue_selected;
		for (int i = 0; i < nets.length; i++)
			nets[i] = NeuralNetwork.load(fname+"/nn"+i+".nnet");
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
		double[] input = Tools.generateAIinput(worldState, blue_selected, my_goal_left);
		for (int i = 0; i < nets.length; i++) {
			nets[i].setInput(input);
			nets[i].calculate();
		}
		int[] results = new int[nets.length];
		// get results
		for (int i = 0; i < results.length; i++)
			results[i] = Tools.recoverOutput(nets[i].getOutput());

		try {
			int speed = 0;
			if (results[0] == 1)
				speed = MAX_SPEED_CM_S;
			else if (results[0] == 2)
				speed = - MAX_SPEED_CM_S;
			
			int turn_speed = 0;
			if (results[1] == 1)
				turn_speed = 127;
			else if (results[1] == 2)
				turn_speed = -127;
			
			mComm.sendMessage(opcode.operate, (byte) speed, (byte) turn_speed);
			if (results[2] == 1)
				mComm.sendMessage(opcode.kick);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
