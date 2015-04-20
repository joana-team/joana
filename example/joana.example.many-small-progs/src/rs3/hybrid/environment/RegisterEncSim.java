package rs3.hybrid.environment;

public class RegisterEncSim {

	public static boolean register(int id, byte[] domain, byte[] publicKey) {
		Environment.untrustedOutput(7901);
		Environment.untrustedOutput(id);
		Environment.untrustedOutputMessage(domain);
		Environment.untrustedOutputMessage(publicKey);
		return Environment.untrustedInput()==0;
	}

	public static boolean getEncryptor(int id, byte[] domain) {
		Environment.untrustedOutput(7902);
		Environment.untrustedOutput(id);
		Environment.untrustedOutputMessage(domain);
		return Environment.untrustedInput()==0;
	}

}
