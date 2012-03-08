package sdp.vision.processing;


/**
 * A container for thresholding bounds.
 * 
 * @author Gediminas Liktaras
 */
public class ThresholdBounds {
	
	/** Different types of values that can be set. */
	public enum ValueType { HUE_MIN, HUE_MAX, SAT_MIN, SAT_MAX, VAL_MIN, VAL_MAX };
	
	/** Lower bound of the hue threshold. */
	private int hueMin = 0;
	/** Upper bound of the hue threshold. */
	private int hueMax = 360;
	/** Lower bound of the saturation threshold. */
	private int satMin = 0;
	/** Upper bound of the saturation threshold. */
	private int satMax = 100;
	/** Lower bound of the value threshold. */
	private int valMin = 0;
	/** Upper bound of the value threshold. */
	private int valMax = 100;
	
	
	/**
	 * Create a new threshold bound container.
	 */
	public ThresholdBounds() { }


	/**
	 * @return the hueMin
	 */
	public int getHueMin() {
		return hueMin;
	}

	/**
	 * @param hueMin the hueMin to set
	 */
	public void setHueMin(int hueMin) {
		this.hueMin = hueMin;
	}


	/**
	 * @return the hueMax
	 */
	public int getHueMax() {
		return hueMax;
	}

	/**
	 * @param hueMax the hueMax to set
	 */
	public void setHueMax(int hueMax) {
		this.hueMax = hueMax;
	}


	/**
	 * @return the satMin
	 */
	public int getSatMin() {
		return satMin;
	}

	/**
	 * @param satMin the satMin to set
	 */
	public void setSatMin(int satMin) {
		this.satMin = satMin;
	}


	/**
	 * @return the satMax
	 */
	public int getSatMax() {
		return satMax;
	}

	/**
	 * @param satMax the satMax to set
	 */
	public void setSatMax(int satMax) {
		this.satMax = satMax;
	}


	/**
	 * @return the valMin
	 */
	public int getValMin() {
		return valMin;
	}

	/**
	 * @param valMin the valMin to set
	 */
	public void setValMin(int valMin) {
		this.valMin = valMin;
	}


	/**
	 * @return the valMax
	 */
	public int getValMax() {
		return valMax;
	}

	/**
	 * @param valMax the valMax to set
	 */
	public void setValMax(int valMax) {
		this.valMax = valMax;
	}
	
	
	/**
	 * Get one of the threshold bound values.
	 * 
	 * @param type The type of value to return.
	 * @return The requested value.
	 */
	public int getValue(ValueType type) {
		switch (type) {
		case HUE_MIN :
			return getHueMin();
		case HUE_MAX :
			return getHueMax();
		case SAT_MIN :
			return getSatMin();
		case SAT_MAX :
			return getSatMax();
		case VAL_MIN :
			return getValMin();
		case VAL_MAX :
			return getValMax();
		default :
			return -1;
		}
	}
	
	/**
	 * Set one of the threshold bound values.
	 * 
	 * @param type The type of value to set.
	 * @param value Value to set the value to (sigh).
	 */
	public void setValue(ValueType type, int value) {
		switch (type) {
		case HUE_MIN :
			setHueMin(value);
			break;
		case HUE_MAX :
			setHueMax(value);
			break;
		case SAT_MIN :
			setSatMin(value);
			break;
		case SAT_MAX :
			setSatMax(value);
			break;
		case VAL_MIN :
			setValMin(value);
			break;
		case VAL_MAX :
			setValMax(value);
			break;
		default :
			break;
		}
	}

}
