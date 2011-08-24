package com.barbermot.pilot;

public class AutoControl implements SignalListener {

	public AutoControl(ControlListener control) {
		this.control = control;
		this.isFirst = true;
	}

	public void update(double value, long time) {
		if (engaged) {

			if (isFirst) {
				isFirst = false;
				lastTime = time;
				lastError = goal - value;
				cummulativeError = 0;
				return;
			}

			double pTotal = 0;
			double iTotal = 0;
			double dTotal = 0;

			double timeDelta = time - lastTime;

			if (timeDelta <= 0) {
				return;
			}

			double error = goal - value;
			double errorDelta = error - lastError;

			// simple adjustment proportional to the error
			pTotal = proportional * error;

			// reacts to the length of an error
			cummulativeError += error * timeDelta;
			if (cummulativeError > maxCummulative) {
				cummulativeError = maxCummulative;
			} else if (cummulativeError < minCummulative) {
				cummulativeError = minCummulative;
			}
			iTotal = integral * cummulativeError;

			// adjustment to react to the closing speed
			dTotal = derivative * (errorDelta / timeDelta);

			lastError = error;
			lastTime = time;

			control.adjust(pTotal + iTotal + dTotal);
		}
	}

	void setConfiguration(double[] conf) {
	    proportional = conf[0];
	    integral = conf[1];
	    derivative = conf[2];
	    minCummulative = conf[3];
	    maxCummulative = conf[4];
	    isFirst = true;
	}

	double getProportional() { 
		return proportional; 
	}
	
	void setProportional(double proportional) { 
		this.proportional = proportional; 
	}

	double getIntegral() { 
		return integral; 
	}
	
	void setIntegral(double integral) { 
		this.integral = integral; 
	}

	double getDerivative() { 
		return derivative; 
	}
	
	void setDerivative(double derivative) { 
		this.derivative = derivative; 
	}

	double getMaxCummulative() { 
		return maxCummulative; 
	}
	
	void setMaxCummulative(double max) { 
		this.maxCummulative = max; 
	}

	double getMinCummulative() { 
		return minCummulative; 
	}
	
	void setMinCummulative(double min) { 
		this.minCummulative = min; 
	}

	double getGoal() { 
		return goal; 
	}
	
	void setGoal(double goal) { 
		this.goal = goal;
		isFirst = true; 
	}

	void engage(boolean engaged) { 
		this.engaged = engaged; 
	}
	
	boolean isEngaged() { 
		return engaged; 
	}

	private ControlListener control;
	private double proportional;
	private double integral;
	private double derivative;
	private double lastError;
	private double lastTime;
	private double cummulativeError;
	private double maxCummulative;
	private double minCummulative;
	private double goal;
	private boolean engaged;
	private boolean isFirst;
}
