package sdp.communicator;

import java.io.IOException;

import sdp.common.Communicator;
import sdp.common.MessageListener;
import sdp.common.Communicator.opcode;

/**
 * Sends test signals to device using a {@link Communicator}
 * @author martinmarinov
 *
 */
public class Test implements MessageListener {
	public static void main(String[] args) throws Exception {
		new Test(); // create a new Test
	}
	
	public Test() throws IOException, InterruptedException{
		Communicator my = null;
		my = new JComm(this); // initialize bluetooth communicator
		// send move message
		my.sendMessage(opcode.move, (byte)2);
		Thread.sleep(12000);
		// after 12 seconds send an exit message
		my.sendMessage(opcode.exit);
	}

	@Override
	public void receiveMessage(opcode op, byte[] args, Communicator controler) {
		// this will get triggered if the brick sends a message back
	}

}
