package sdp.AI.neural;

import java.io.IOException;

import org.neuroph.core.NeuralNetwork;

import sdp.AI.AI;
import sdp.common.Communicator;
import sdp.common.Tools;
import sdp.common.WorldStateProvider;
import sdp.common.Communicator.opcode;

public class AINeuralNetwork extends AI {
	
	private final static int network_count = 2;
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
		for (int i = 0; i < results.length; i++) {
			results[i] = Tools.recoverOutput(nets[i].getOutput());
			if (results[i] == -1) {
				System.out.println("The network is misbehaving");
				return;
			}
		}
		System.out.println(results[0]+" with confidence "+String.format("%.2f", 100d*Tools.probability(results[0], nets[0].getOutput()))+"%");
		try {
			int speed = 0, turn_speed = 0;
			switch (results[0]) {
			case 0:
				speed = 35;
				turn_speed = 90;
				break;
			case 1:
				speed = 35;
				turn_speed = -90;
				break;
			case 2:
				speed = 35;
				turn_speed = 0;
				break;
			case 3:
				speed = -35;
				turn_speed = 90;
				break;
			case 4:
				speed = -35;
				turn_speed = -90;
				break;
			case 5:
				speed = -35;
				turn_speed = 0;
				break;
			case 6:
				speed = 0;
				turn_speed = 90;
				break;
			case 7:
				speed = 0;
				turn_speed = -90;
				break;
			case 8:
				speed = 0;
				turn_speed = 0;
				break;

			}
			mComm.sendMessage(opcode.operate, (byte) speed, (byte) turn_speed);
			if (results[1] == 1)
				mComm.sendMessage(opcode.kick);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
