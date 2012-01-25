package sdp.brick;

import sdp.common.Communicator;
import sdp.common.MessageListener;
import sdp.common.Communicator.opcode;

import lejos.nxt.Battery;
import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.NXT;

/**
 * This is the program that should be uploaded to the NXT Brick.
 * It translates PC commands into movements. Modify {@link #receiveMessage(opcode, byte[], Communicator)}
 * method to add new abilities. To add new opcodes, modify {@link opcode}.
 * 
 * @author martinmarinov
 *
 */
public class Brick {



	private static Communicator mCont;

	/**
	 * The entry point of the program
	 * @param args
	 */
	public static void main(String[] args) {
		// connect with PC and start receiving messages
		mCont = new BComm(new MessageListener() {
			public static final float CIRC = 0.4712F;
			public static final float WHEEL = 0.26F;
			int lastCountA = 0;
			int lastCountC = 0;
			float slowest = Motor.A.getMaxSpeed() > Motor.B.getMaxSpeed() ? Motor.B.getMaxSpeed()-10 : Motor.A.getMaxSpeed()-10;

			/**
			 * Add your movement logic inside this method
			 */
			@Override
			public void receiveMessage(opcode op, byte[] args, Communicator controler) {
				// to send messages back to PC, use mCont.sendMessage
				switch (op) {

				case exit:
					mCont.close();
					NXT.shutDown();
					break;

				case move:
					if (args.length > 0) {					
						Motor.A.setSpeed(slowest);
						Motor.C.setSpeed(slowest);
						Motor.A.setAcceleration(args[1]*100);
						Motor.C.setAcceleration(args[1]*100);
						Motor.A.forward();
						Motor.C.forward();
						try {
							Thread.sleep(args[0]*500);
							LCD.drawString("M-A: " + (Motor.A.getTachoCount()-lastCountA), 2, 3);
							LCD.drawString("M-C: " + (Motor.C.getTachoCount()-lastCountC), 2, 4);
							lastCountA = Motor.A.getTachoCount();
							lastCountC = Motor.C.getTachoCount();
							Thread.sleep(args[0]*500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						Motor.A.setSpeed(0);
						Motor.C.setSpeed(0);
						Motor.C.stop();
						Motor.A.stop();
					}
					break;

				case moveback:
					if (args.length > 0) {
						Motor.A.setSpeed(slowest);
						Motor.C.setSpeed(slowest);
						Motor.A.backward();
						Motor.C.backward();

						try {
							Thread.sleep(args[0]*1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						Motor.C.setSpeed(0);
						Motor.A.setSpeed(0);
						Motor.C.stop();
						Motor.A.stop();
					}
					break;

				case kick:
					Motor.B.setSpeed(Motor.B.getMaxSpeed());
					Motor.B.setAcceleration(100000);
					Motor.B.rotate(-120);
					Motor.B.rotate(120);
					Motor.B.stop();
					break;

				case turn:
					if (args.length > 0) {
						int a =(int) ((CIRC*(args[0]/360))/WHEEL);
						Motor.A.setSpeed(360);
						Motor.C.setSpeed(360);
						Motor.A.rotate(a);
						Motor.C.rotate(a);
					}

					break;

				case rotate_kicker:
					if (args.length > 0) {
						Motor.B.setSpeed(slowest);
						Motor.B.setAcceleration(100000);
						Motor.B.rotate(args[0]);
					}
					break;
				}

			}
		});
	}
}
