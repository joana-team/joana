package rs3.hybrid.environment;

public class SMTEnv {

	public static boolean registerSender(int id)	{
		Environment.untrustedOutput(7800);
		Environment.untrustedOutput(id);
		return Environment.untrustedInput()==0;
	}

	public static boolean registerReceiver(int id)	{
		Environment.untrustedOutput(7801);
		Environment.untrustedOutput(id);
		return Environment.untrustedInput()==0;
	}

	public static byte[] sendTo(int message_length, int sender_id, int receiver_id, String server, int port) {
		Environment.untrustedOutput(7803);
		Environment.untrustedOutput(message_length);
		Environment.untrustedOutput(sender_id);
		Environment.untrustedOutput(receiver_id);
		Environment.untrustedOutputString(server);
		Environment.untrustedOutput(port); 
		return Environment.untrustedInputMessage();
	}

	public static int getMessage(int id, int port) {
		Environment.untrustedOutput(7804);
		Environment.untrustedOutput(id);
		Environment.untrustedOutput(port);
		return Environment.untrustedInput();
	}

	public static boolean listenOn(int port) {
		Environment.untrustedOutput(7805);
		Environment.untrustedOutput(port);
		return Environment.untrustedInput()==0;
	}
}
