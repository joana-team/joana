package rs3.cloud.cloudStorage;

import rs3.cloud.lib.network.NetworkClient;
import rs3.cloud.lib.network.NetworkError;

public class NetworkReal implements NetworkInterface {
	public byte[] sendRequest(byte[] msg) throws NetworkError {
		return NetworkClient.sendRequest(msg, Params.SERVER_NAME, Params.SERVER_PORT);
	}
}
