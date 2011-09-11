package com.barbermot.pilot.pid;

import ioio.lib.api.exception.ConnectionLostException;

import com.barbermot.pilot.signal.SignalListener;

public class AutoControl implements SignalListener {

	private ControlListener control;
	private float proportional;
	private float integral;
	private float derivative;
	private float lastError;
	private float lastTime;
	private float cummulativeError;
	private float maxCummulative;
	private float minCummulative;
	private boolean engaged;
	private boolean isFirst;
	protected float goal;

	public AutoControl(ControlListener control) {
		this.control = control;
		this.isFirst = true;
	}

	public void update(float value, long time) throws ConnectionLostException {
		if (engaged) {

			if (isFirst) {
				isFirst = false;
				lastTime = time;
				lastError = computeError(value);
				cummulativeError = 0;
				return;
			}

			float pTotal = 0;
			float iTotal = 0;
			float dTotal = 0;

			float timeDelta = time - lastTime;

			if (timeDelta <= 0) {
				return;
			}

			float error = computeError(value);
			float errorDelta = error - lastError;

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

	protected float computeError(float value) {
		return goal - value;
	}

	public void setConfiguration(float[] conf) {
		proportional = conf[0];
		integral = conf[1];
		derivative = conf[2];
		minCummulative = conf[3];
		maxCummulative = conf[4];
		isFirst = true;
	}

	public float getProportional() {
		return proportional;
	}

	public void setProportional(float proportional) {
		this.proportional = proportional;
	}

	public float getIntegral() {
		return integral;
	}

	public void setIntegral(float integral) {
		this.integral = integral;
	}

	public float getDerivative() {
		return derivative;
	}

	public void setDerivative(float derivative) {
		this.derivative = derivative;
	}

	public float getMaxCummulative() {
		return maxCummulative;
	}

	public void setMaxCummulative(float max) {
		this.maxCummulative = max;
	}

	public float getMinCummulative() {
		return minCummulative;
	}

	public void setMinCummulative(float min) {
		this.minCummulative = min;
	}

	public float getGoal() {
		return goal;
	}

	public void setGoal(float goal) {
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
