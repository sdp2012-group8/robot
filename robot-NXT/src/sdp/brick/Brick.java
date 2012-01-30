package sdp.brick;

import java.io.File;

import sdp.common.Communicator;
import sdp.common.MessageListener;
import sdp.common.Communicator.opcode;

<<<<<<< HEAD
import lejos.nxt.Battery;
=======
>>>>>>> b96687b875cf2d04bb4f5823a5b6b7b6fd42ea74
import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.NXT;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.TouchSensor;
import lejos.nxt.UltrasonicSensor;

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

			// for joypad
			private float speed_a = 0;
			private float speed_c = 0;

			// for tacho

			public static final float ROBOTR = 7.1F;
			public static final float WHEELR = 4F;
			int lastCountA = 0;
			int lastCountC = 0;
			float slowest = Motor.A.getMaxSpeed() > Motor.B.getMaxSpeed() ? Motor.B.getMaxSpeed()-10 : Motor.A.getMaxSpeed()-10;

			/**
			 * Add your movement logic inside this method
			 */
			@Override
			public void receiveMessage(opcode op, byte[] args, Communicator controler) {
				final int def_vol = Sound.getVolume();
				// to send messages back to PC, use mCont.sendMessage
				float max;
				switch (op) {

				case checkTouch:
					TouchSensor tsens = new TouchSensor(SensorPort.S2);
					TouchSensor tsens2 = new TouchSensor(SensorPort.S3);
					while(true) {
						int i = 0;
						if (tsens.isPressed() || tsens2.isPressed()){
							LCD.drawString("tSensor is true", 2, 4);
							i++;
						} else {
							LCD.drawString("tSensor is false", 2, 4);
						}
						if (i>10) break;
					}

				case exit:
					mCont.close();
					Sound.setVolume(def_vol);
					NXT.shutDown();
					break;

				case move:
<<<<<<< HEAD
					try {
						max = Battery.getVoltage()*100;
						Motor.A.setSpeed(max+args[2]);
						Motor.C.setSpeed(max+args[3]);
=======
					if (args.length > 0) {					
						Motor.A.setSpeed(slowest);
						Motor.C.setSpeed(slowest);
>>>>>>> b96687b875cf2d04bb4f5823a5b6b7b6fd42ea74
						Motor.A.setAcceleration(args[1]*100);
						Motor.C.setAcceleration(args[1]*100);
						Motor.A.forward();
						Motor.C.forward();
<<<<<<< HEAD
					} catch (Exception e){
						LCD.drawString("Command Error: Check Args", 2, 2);
					}
					try {
						Thread.sleep(args[0]*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
=======
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
>>>>>>> b96687b875cf2d04bb4f5823a5b6b7b6fd42ea74
					}
					break;

				case moveback:
<<<<<<< HEAD
					try {
						max = Battery.getVoltage()*100;
						Motor.A.setSpeed(max);
						Motor.C.setSpeed(max);
						Motor.A.setAcceleration(args[1]*100);
						Motor.C.setAcceleration(args[1]*100);
						Motor.A.backward();
						Motor.C.backward();
					} catch (Exception e){
						LCD.drawString("Command Error: Check Args", 2, 2);
					}
					try {
						Thread.sleep(args[0]*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
=======
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
>>>>>>> b96687b875cf2d04bb4f5823a5b6b7b6fd42ea74
					}
					break;
<<<<<<< HEAD
					
				case moveangle:
					
						Motor.A.rotate(args[0], true);
						Motor.C.rotate(args[0], true);
					try {
						Thread.sleep(args[0]*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					break;	
				case kick:
						max = Battery.getVoltage()*100;
						Motor.B.setSpeed(max);
						Motor.B.setAcceleration(100000);
						Motor.B.rotate(-70);
						Motor.B.rotate(70);
						Motor.B.stop();
=======

				case kick:
					Motor.B.setSpeed(Motor.B.getMaxSpeed());
					Motor.B.setAcceleration(100000);
					Motor.B.rotate(-80);
					Motor.B.rotate(80);
					Motor.B.stop();
>>>>>>> b96687b875cf2d04bb4f5823a5b6b7b6fd42ea74
					break;

				case turn:
					if (args.length > 0) {
						int a =(int) ((args[0]*ROBOTR)/WHEELR);
						Motor.A.setSpeed(360);
						Motor.C.setSpeed(360);
						Motor.A.rotate(a, true);
						Motor.C.rotate(-a, true);
					}

					break;

				case rotate_kicker:
<<<<<<< HEAD
						max = Battery.getVoltage()*100;
						Motor.B.setSpeed(max);
						Motor.B.setAcceleration(100000);
						Motor.B.rotate(args[0]);
					break;
				default:
					LCD.drawString("Unknown Command", 2, 2);
=======
					if (args.length > 0) {
						Motor.B.setSpeed(slowest);
						Motor.B.setAcceleration(100000);
						Motor.B.rotate(args[0]);
					}
>>>>>>> b96687b875cf2d04bb4f5823a5b6b7b6fd42ea74
					break;

				case move_to_wall:
					UltrasonicSensor sens = new UltrasonicSensor(SensorPort.S1);
					sens.continuous();
					Motor.A.setSpeed(slowest);
					Motor.C.setSpeed(slowest);
					Motor.A.setAcceleration(1000);
					Motor.C.setAcceleration(1000);
					Motor.A.forward();
					Motor.C.forward();

					while (true) { // if no other sensors interfere?
						int dist = sens.getDistance();
						LCD.clear(2);
						LCD.clear(3);
						LCD.drawString(String.valueOf(dist), 0, 2);
						LCD.drawString(sens.getUnits(), 0, 3);
						if (dist < 30)
							break;
					}
					Motor.A.setSpeed(0);
					Motor.C.setSpeed(0);
					Motor.C.stop();
					Motor.A.stop();
					sens.off();
					break;

				case operate:
					// args[0] - speed in cm per second
					// args[1] - turning speed in degrees per second around center of robot
					if (args.length > 0) {
						float old_a = speed_a;
						float old_c = speed_c;
						// convert the degrees per second around robot
						// to degrees per second for the motor
						float conv_angle = 0;
						if (args.length > 1) conv_angle = args[1]*ROBOTR/WHEELR;
						float speed_angle = args[0]/(0.017453292519943295f*WHEELR); // Radius*Pi*Angle/180
						// set desired speed
						speed_a = speed_angle+conv_angle;
						speed_c = speed_angle-conv_angle;
						// change speed according to turning
						Motor.A.setSpeed(Math.abs(speed_a));
						Motor.C.setSpeed(Math.abs(speed_c));
						// check if we need to start motors or turn their direction
						if (old_a == 0 || old_a*speed_a < 0) {
							Motor.A.setAcceleration(1000);
							if (speed_a > 0)
								Motor.A.forward();
							else
								Motor.A.backward();
						}
						// check if we need to start motors or turn their direction
						if (old_c == 0 || old_c*speed_c < 0) {
							Motor.C.setAcceleration(1000);
							if (speed_c > 0)
								Motor.C.forward();
							else
								Motor.C.backward();
						}
						// check if we need to stop motors
						if (speed_a == 0)
							Motor.A.stop();
						if (speed_c == 0)
							Motor.C.stop();
					}
					break;

				case play_sound:
					Sound.setVolume(Sound.VOL_MAX);
					Sound.playSample(new File("SMB.wav"));
					break;

				}
			}
		});
		// if the communicator is not listening inside a new thread, this code below this point will never be reached!
	}
}
