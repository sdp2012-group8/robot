package sdp.AI.neural;

import java.io.IOException;

import org.neuroph.core.NeuralNetwork;

import sdp.AI.AI;
import sdp.common.Communicator;
import sdp.common.Robot;
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
		Robot me = blue_selected ? worldState.getBlueRobot() : worldState.getYellowRobot();
		Robot enemy = blue_selected ? worldState.getYellowRobot() : worldState.getBlueRobot();
		neuralNetwork.setInput(									
				me.getCoords().getX(),
				me.getCoords().getY(),
				enemy.getCoords().getX(),
				enemy.getCoords().getY(),
				worldState.getBallCoords().getX(),
				worldState.getBallCoords().getY());
		neuralNetwork.calculate();
		double[] output = neuralNetwork.getOutput();
		System.out.println("0 : "+output[0]+"; 1 : "+output[1]+"; 2 : "+output[2]);
		try {
			mComm.sendMessage(opcode.operate, (byte) output[0], (byte) output[1]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
