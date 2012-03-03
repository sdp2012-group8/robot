package sdp.vision.processing;


/**
 * A container for thresholding bounds.
 * 
 * @author Gediminas Liktaras
 */
public class ThresholdBounds {
	
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
	/** Lower bound of the bounding box size threshold. */
	private int sizeMin = 1;
	/** Upper bound of the bounding box size threshold. */
	private int sizeMax = 100;
	
	
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
	 * @return the sizeMin
	 */
	public int getSizeMin() {
		return sizeMin;
	}

	/**
	 * @param sizeMin the sizeMin to set
	 */
	public void setSizeMin(int sizeMin) {
		this.sizeMin = sizeMin;
	}


	/**
	 * @return the sizeMax
	 */
	public int getSizeMax() {
		return sizeMax;
	}

	/**
	 * @param sizeMax the sizeMax to set
	 */
	public void setSizeMax(int sizeMax) {
		this.sizeMax = sizeMax;
	}

}
