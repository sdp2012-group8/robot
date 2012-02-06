package sdp.AI.neural;

import java.io.IOException;

import org.neuroph.core.NeuralNetwork;

import sdp.AI.AI;
import sdp.common.Communicator;
import sdp.common.Tools;
import sdp.common.WorldStateProvider;
import sdp.common.Communicator.opcode;

public class AINeuralNetwork extends AI {
	
	private final static int network_count = 5;
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
		boolean is_going_forwards	= Tools.recoverOutput(nets[0].getOutput()),
				is_standing_still 	= Tools.recoverOutput(nets[1].getOutput()),
				is_turning_right 	= Tools.recoverOutput(nets[2].getOutput()),
				is_not_turning 		= Tools.recoverOutput(nets[3].getOutput()),
				is_it_kicking 		= Tools.recoverOutput(nets[4].getOutput());
		System.out.println(is_going_forwards+" "+is_standing_still+" "+is_turning_right+" "+is_not_turning+" "+is_it_kicking);
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
