package rs3.cloud.cloudStorage;

import rs3.cloud.lib.network.*;

public class NetworkReal implements NetworkInterface {
	public byte[] sendRequest(byte[] msg) throws NetworkError {
		return NetworkClient.sendRequest(msg, Params.SERVER_NAME, Params.SERVER_PORT);
	}
}
