package com.barbermot.pilot;

import ioio.lib.api.exception.ConnectionLostException;

import java.util.NoSuchElementException;
import java.util.Scanner;

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
			case 'c':
			{
				int type;

				double proportional;
				double integral;
				double derivative;
				double min;
				double max;
				
				try {
					type = scanner.nextInt();
					proportional = scanner.nextDouble();
					integral = scanner.nextDouble();
					derivative = scanner.nextDouble();
					min = scanner.nextDouble();
					max = scanner.nextDouble();
				} catch(NoSuchElementException e) {
					fail();
					return;
				}

				double[] conf = {proportional,integral,derivative,min,max};
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
					computer.stabilize(x==0?false:true);
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
