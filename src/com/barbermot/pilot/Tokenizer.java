package com.barbermot.pilot;

import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import android.util.Log;

public class Tokenizer {
	
	public static String TAG = "Tokenizer";
	
	public Tokenizer(IOIO ioio, char delim, int rxPin, PrintStream printer) throws ConnectionLostException {
		in = ioio.openUart(rxPin, IOIO.INVALID_PIN, 9600, Uart.Parity.NONE, Uart.StopBits.ONE).getInputStream();
		this.delim = delim;
		this.printer = printer;
		this.buffer = new StringBuffer();
	}
	
	public String read() throws IOException {
		char c;
		String result = null;
		
	    while (in.available() > 0) {
	        c = (char) in.read();

	        if (startToken && Character.isWhitespace(c)) {
	            continue;
	        } else {
	            startToken = false;
	        }

	        if (c == delim) {
	            startToken = true;
	            result = buffer.toString();
	            printer.println(result);
	            printer.flush();
	            buffer = new StringBuffer();
	            break;
	        } else {
	            buffer.append(c);
	        }
	    }
	    
	    return result;
	}
	
	private PrintStream printer;
	private char delim;
	private boolean startToken;
	private InputStream in;
	private StringBuffer buffer;
}
