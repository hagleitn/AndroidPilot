package com.barbermot.pilot;

import ioio.lib.api.exception.ConnectionLostException;

public class AutoControl implements SignalListener {
	
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

	public AutoControl(ControlListener control) {
		this.control = control;
		this.isFirst = true;
	}

	public void update(double value, long time) throws ConnectionLostException {
		if (engaged) {

			if (isFirst) {
				isFirst = false;
				lastTime = time;
				lastError = computeError(value);
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

			double error = computeError(value);
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
	
	protected double computeError(double value) {
		return goal - value;
	}

	public void setConfiguration(double[] conf) {
	    proportional = conf[0];
	    integral = conf[1];
	    derivative = conf[2];
	    minCummulative = conf[3];
	    maxCummulative = conf[4];
	    isFirst = true;
	}

	public double getProportional() { 
		return proportional; 
	}
	
	public void setProportional(double proportional) { 
		this.proportional = proportional; 
	}

	public double getIntegral() { 
		return integral; 
	}
	
	public void setIntegral(double integral) { 
		this.integral = integral; 
	}

	public double getDerivative() { 
		return derivative; 
	}
	
	public void setDerivative(double derivative) { 
		this.derivative = derivative; 
	}

	public double getMaxCummulative() { 
		return maxCummulative; 
	}
	
	public void setMaxCummulative(double max) { 
		this.maxCummulative = max; 
	}

	public double getMinCummulative() { 
		return minCummulative; 
	}
	
	public void setMinCummulative(double min) { 
		this.minCummulative = min; 
	}

	public double getGoal() { 
		return goal; 
	}
	
	public void setGoal(double goal) { 
		this.goal = goal;
		isFirst = true; 
	}

	public void engage(boolean engaged) { 
		this.engaged = engaged; 
	}
	
	public boolean isEngaged() { 
		return engaged; 
	}
}
