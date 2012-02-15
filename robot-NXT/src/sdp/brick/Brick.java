package sdp.brick;

import java.io.File;

import sdp.common.Communicator;
import sdp.common.MessageListener;
import sdp.common.Communicator.opcode;


import lejos.nxt.Battery;

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
		mCont = new BComm();
		mCont.registerListener(new MessageListener() {

			// for joypad
			private float speed_a = 0;
			private float speed_c = 0;

			// for conversions
			private int acc = 1000; // acc in degrees/s/s
			private double acceleration = acc*0.017453292519943295*WHEELR;//69.8;
			private double turn_acceleration = acc*WHEELR/ROBOTR;

			// for tacho

			public static final float ROBOTR = 7.1F;
			public static final float WHEELR = 4F;
			float slowest = Motor.A.getMaxSpeed() > Motor.B.getMaxSpeed() ? Motor.B.getMaxSpeed()-10 : Motor.A.getMaxSpeed()-10;

			/**
			 * Add your movement logic inside this method
			 */
			@Override
			public void receiveMessage(opcode op, byte[] args, Communicator controller) {
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

				case kick:
					max = Battery.getVoltage()*100;
					Motor.B.setSpeed(max);
					Motor.B.setAcceleration(100000);
					Motor.B.rotate(-70);
					Motor.B.rotate(70);
					Motor.B.stop();

					break;

				case rotate_kicker:
					if (args.length > 0) {
						Motor.B.setSpeed(slowest);
						Motor.B.setAcceleration(100000);
						Motor.B.rotate(args[0]);
					}
					break;

				case rotate_kicker_lock:
					if (args.length > 0) {
						Motor.B.setSpeed(slowest);
						Motor.B.setAcceleration(100000);
						Motor.B.rotate(args[0]);
					}
					Motor.B.lock(100);
					break;

				case float_motor:
					Motor.B.flt();
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
					// args[1] - turning speed in degrees per second around centre of robot
					// args[2] - acceleration in cm/s/s
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
						if (args.length > 2) {
							//change acceleration
							acceleration = args[2];
							acc = (int) (acceleration/(0.017453292519943295*WHEELR));
							turn_acceleration = acc*WHEELR/ROBOTR;
						}
						if (old_a == 0 || old_a*speed_a < 0) {
							Motor.A.setAcceleration(acc);
							if (speed_a > 0)
								Motor.A.forward();
							else
								Motor.A.backward();
						}
						// check if we need to start motors or turn their direction
						if (old_c == 0 || old_c*speed_c < 0) {
							Motor.C.setAcceleration(acc);
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
