/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.util.jobber.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import edu.kit.joana.wala.util.jobber.io.CmdMessage;
import edu.kit.joana.wala.util.jobber.io.IOUtils;
import edu.kit.joana.wala.util.jobber.io.MessageParseException;
import edu.kit.joana.wala.util.jobber.io.RespMessage;
import edu.kit.joana.wala.util.jobber.server.JobberServer;

abstract class JobberClient extends Thread {

	private final int port;
	private final String serverIp;

	protected JobberClient(String serverIp, int port) {
		if (serverIp == null) {
			throw new IllegalArgumentException("Server name has to be provided.");
		}

		this.port = port;
		this.serverIp = serverIp;
	}

	protected JobberClient(String serverIp) {
		this(serverIp, JobberServer.PORT);
	}


	protected RespMessage send(final CmdMessage cmd) throws UnknownHostException, IOException, MessageParseException {
		final Socket soc = new Socket(serverIp, port);
		final BufferedReader in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
		final PrintWriter out = new PrintWriter(soc.getOutputStream());

		IOUtils.sendMessage(out, cmd);

		RespMessage result = IOUtils.readResponse(in);

		in.close();
		out.close();

		if (!soc.isClosed()) {
			soc.close();
		}

		return result;
	}

	public String toString() {
		return "{" + serverIp + ":" + port + "}";
	}

	public int getServerPort() {
		return port;
	}

	public String getServerIp() {
		return serverIp;
	}

}
