package com.barbermot.pilot;

import java.io.IOException;
import java.io.PrintStream;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

public class SerialController {
	
	public SerialController(IOIO ioio, FlightComputer computer, char delim, int rxPin, PrintStream printer) 
			throws ConnectionLostException {
		token = new Tokenizer(ioio, delim, rxPin, printer);
		parser = new Parser(computer);
	}
	
	public void executeCommand() throws IOException, ConnectionLostException {
		if (startSleep != 0) {
			if (System.currentTimeMillis()-startSleep < sleepTime) {
				return;
			} else {
				sleepTime = 0;
				startSleep = 0;
			}
		}

		String cmd = token.read();
		if (null != cmd) {
			if (cmd.charAt(0) == 'z' || cmd.charAt(0) == 'Z') {
				int x = 0;
				cmd = cmd.substring(1);
				cmd.trim();
				try {
					x = Integer.parseInt(cmd);
					sleepTime = x;
					startSleep = System.currentTimeMillis();
				} catch (NumberFormatException e) {
					parser.fail();
				}
			} else {
				parser.doCmd(cmd);
			}
		}
	}
	
	private long startSleep;
	private long sleepTime;
	private Parser parser;
	private Tokenizer token;
}
