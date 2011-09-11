package com.barbermot.pilot.parser;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.NoSuchElementException;
import java.util.Scanner;

import com.barbermot.pilot.flight.FlightComputer;

public class Parser {

	public Parser(FlightComputer computer) {
		this.computer = computer;
	}

	public void doCmd(String cmd) throws ConnectionLostException {
		int x = 0;
		Scanner scanner = new Scanner(cmd);

		if (scanner.hasNext(".")) {
			char c = scanner.next(".").charAt(0);
			switch (c) {

			// Command "H <int>" hovers the thing at altitude <int>
			case 'h':
			case 'H':
				if (scanner.hasNextInt()) {
					x = scanner.nextInt();
					computer.hover(x);
				} else {
					fail();
				}
				break;

			// Command "T <int>" takeoff and start hovering at <int>
			case 't':
			case 'T':
				if (scanner.hasNextInt()) {
					x = scanner.nextInt();
					computer.takeoff(x);
				} else {
					fail();
				}
				break;

			// Command "L" lands the thing
			case 'l':
			case 'L':
				computer.land();
				break;

			case 'C':
			case 'c': {
				int type;

				float proportional;
				float integral;
				float derivative;
				float min;
				float max;

				try {
					type = scanner.nextInt();
					proportional = scanner.nextFloat();
					integral = scanner.nextFloat();
					derivative = scanner.nextFloat();
					min = scanner.nextFloat();
					max = scanner.nextFloat();
				} catch (NoSuchElementException e) {
					fail();
					return;
				}

				float[] conf = { proportional, integral, derivative, min, max };
				switch (type) {
				case 1:
					computer.setHoverConfiguration(conf);
					break;
				case 2:
					computer.setLandingConfiguration(conf);
					break;
				case 3:
					computer.setStabilizerConfiguration(conf);
					break;
				default:
					fail();
					break;
				}
			}
				break;

			// (Re-)Engage auto throttle
			case 'e':
			case 'E':
				computer.autoControl();
				break;

			// Command "S" turns on/off stabilization
			case 's':
			case 'S':
				if (scanner.hasNextInt()) {
					x = scanner.nextInt();
					computer.stabilize(x == 0 ? false : true);
				} else {
					fail();
				}
				break;

			// Set minimum throttle
			case 'm':
			case 'M':
				if (scanner.hasNextInt()) {
					x = scanner.nextInt();
					computer.setMinThrottle(x);
				} else {
					fail();
				}
				break;

			// Set maximum throttle
			case 'n':
			case 'N':
				if (scanner.hasNextInt()) {
					x = scanner.nextInt();
					computer.setMaxThrottle(x);
				} else {
					fail();
				}
				break;

			// Commands "X" stops the thing
			case 'x':
			case 'X':
				computer.abort();
				break;
			default:
				fail();
				break;
			}
		}

	}

	public void fail() {

	}

	private FlightComputer computer;
}
