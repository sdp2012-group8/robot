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
		nets[0].setInput(Tools.generateAIinput(worldState, blue_selected, my_goal_left, 0));
		nets[1].setInput(Tools.generateAIinput(worldState, blue_selected, my_goal_left, 1));
		int[] results = new int[nets.length];
		// get results
		for (int i = 0; i < results.length; i++) {
			nets[i].calculate();
			double[] out = nets[i].getOutput();
			results[i] = Tools.recoverOutput(out);
			
			if (results[i] == -1) {
				System.out.println("The network is misbehaving");
				return;
			}
		}
		System.out.println(results[0]+" with confidence "+String.format("%.2f", 100d*Tools.probability(results[0], nets[0].getOutput()))+"%");
		try {
			// 35 and 90
			int speed = results[0] == 0 ? 0 : (results[0] == 1 ? 35 : -35);
			int turn_speed = results[1] == 0 ? 0 : (results[1] == 1 ? 90 : -90);
			mComm.sendMessage(opcode.operate, (byte) speed, (byte) turn_speed);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
