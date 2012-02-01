package sdp.simulator;

import java.io.IOException;
import java.util.ArrayList;

import sdp.common.Communicator;
import sdp.common.MessageListener;

/**
 * 
 * This is a virtual brick - communicator
 * 
 * @author s0932707
 *
 */
public class VBrick implements Communicator {
	
	private ArrayList<MessageListener> mListener = new ArrayList<MessageListener>();

	@Override
	public void sendMessage(opcode op, byte... args) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void registerListener(MessageListener listener) {
		if (!mListener.contains(listener))
			mListener.add(listener);
	}

}
