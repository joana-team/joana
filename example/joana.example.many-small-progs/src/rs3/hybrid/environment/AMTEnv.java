package rs3.hybrid.environment;


public class AMTEnv {
	public static boolean registerSender(int id)	{
		Environment.untrustedOutput(7901);
		Environment.untrustedOutput(id);
		return Environment.untrustedInput()==0;
	}

	public static byte[] sendTo(byte[] message, int sender_id, int recipient_id, String server, int port) {
		Environment.untrustedOutput(7903);
		Environment.untrustedOutputMessage(message);
		Environment.untrustedOutput(sender_id);
		Environment.untrustedOutput(recipient_id);
		Environment.untrustedOutputString(server);
		Environment.untrustedOutput(port);
		return Environment.untrustedInputMessage();
	}

	public static int getMessage(int id, int port) {
		Environment.untrustedOutput(7904);
		Environment.untrustedOutput(id);
		Environment.untrustedOutput(port);
		return Environment.untrustedInput();
	}

	public static boolean listenOn(int port) {
		Environment.untrustedOutput(7905);
		Environment.untrustedOutput(port);
		return Environment.untrustedInput()==0;
	}
}
