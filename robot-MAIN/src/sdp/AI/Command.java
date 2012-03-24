package sdp.AI;

import sdp.common.Robot;
import sdp.common.Utilities;


/**
 * A robot command.
 * 
 * TODO: Could someone encapsulate the variables with getters/setters when
 * there is more time?
 */
public class Command {
	
	/** Robot's acceleration speed. */
	public double acceleration = Robot.ACCELERATION_SPEED;
	/** Robot's forward driving speed in cm/s. */
	public double drivingSpeed = 0.0;
	/** Robot's turning speed in degrees/s. */
	public double turningSpeed = 0.0;

	/** Whether should attempt to kick. */
	public boolean kick = false;
	
	
	/**
	 * Create a new robot command.
	 * 
	 * @param drivingSpeed Robot's forward driving speed.
	 * @param turningSpeed Robot's turning speed.
	 * @param kick Whether the robot should be ready to kick.
	 */
	public Command(double drivingSpeed, double turningSpeed, boolean kick) {
		this.drivingSpeed = drivingSpeed;
		this.turningSpeed = turningSpeed;
		this.kick = kick;
	}
	
	
	/**
	 * Get the robot's driving speed as a short.
	 * 
	 * @return Robot's driving speed as a short.
	 */
	public short getShortDrivingSpeed() {
		return Utilities.restrictToRobotSpeed(Utilities.restrictToShort(drivingSpeed));
	}
	
	/**
	 * Get the robot's turning speed as a short.
	 * 
	 * @return Robot's turning speed as a short.
	 */
	public short getShortTurningSpeed() {
		return Utilities.restrictToShort(turningSpeed);
	}
	
	/**
	 * Get the robot's acceleration as a short.
	 * 
	 * @return Robot's acceleration as a short.
	 */
	public short getShortAcceleration() {
		return Utilities.restrictToShort(acceleration);
	}
	
	
	/**
	 * Get whether the command leaves the acceleration speed intact.
	 * 
	 * @return Whether default robot acceleration is unchanged.
	 */
	public boolean isAccelerationDefault() {
		return Utilities.areDoublesEqual(acceleration, Robot.ACCELERATION_SPEED);
	}

	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "dspd: " + getShortDrivingSpeed() + ", tspd: " + getShortTurningSpeed()
				+ ", kick " + kick + ", acc: " + getShortAcceleration();
	}
}