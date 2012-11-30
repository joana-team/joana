/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.CharBuffer;

public class IOUtils {

	public static final long POLL_RATE = 50;

	private IOUtils() {}

	public static void sendMessage(final PrintWriter out, final Message msg) {
		out.println(msg.getHeaderStr());

		if (msg.hasData()) {
			final CharBuffer data = msg.getData();
			sendData(out, data);
		}

		out.flush();
	}

	public static RespMessage readResponse(final BufferedReader in) throws MessageParseException {
		final String str = readLine(in);

		final RespMessage resp = RespMessage.parse(str);

		if (resp.hasData()) {
			final CharBuffer data = readData(in);
			resp.setData(data);
		}

		return resp;
	}

	public static CmdMessage readCommand(final BufferedReader in) throws MessageParseException {
		final String cmd = readLine(in);

		final CmdMessage msg = CmdMessage.parse(cmd);

		if (msg.hasData()) {
			final CharBuffer data = readData(in);
			msg.setData(data);
		}

		return msg;
	}

	private static String readLine(final BufferedReader in) throws MessageParseException {
		String str = null;

		try {
			while (str == null) {
				str = in.readLine();
				try {
					Thread.sleep(POLL_RATE);
				} catch (InterruptedException e) {}
			}

			if (str != null) {
				str = str.trim();
			}
		} catch (IOException exc) {
			throw new MessageParseException(exc);
		}

		return str;
	}

	private static void sendData(final PrintWriter out, final CharBuffer data) {
		if (data != null) {
			final int savedPos = data.position();

			out.println(data.length());
			final char[] buf = new char[data.length()];
			data.get(buf);
			out.write(buf);

			data.position(savedPos);
		} else {
			// we send data of size 0 bytes
			out.println(0);
		}
	}

	private static CharBuffer readData(final BufferedReader in) throws MessageParseException {
		final String byteNum = readLine(in);

		int numOfBytes = -1;

		try {
			numOfBytes = Integer.parseInt(byteNum);
		} catch (NumberFormatException exc) {
			throw new MessageParseException(exc);
		}

		if (numOfBytes < 0) {
			throw new MessageParseException("Number of bytes has to be >= 0.");
		}

		CharBuffer buf = null;

		try {
			buf = readBytes(in, numOfBytes);
		} catch (IOException exc) {
			throw new MessageParseException(exc);
		}

		return buf;
	}

	private static CharBuffer readBytes(final BufferedReader in, final int num) throws IOException {
		final CharBuffer cbuf = CharBuffer.allocate(num);
		final char[] buf = new char[1024];
		int todo = num;

		while (todo > 0) {
			final int done = in.read(buf);

			if (done > 0) {
				cbuf.put(buf, 0, done);
				todo -= done;
			}
		}

		cbuf.position(0);

		return cbuf;
	}


}
