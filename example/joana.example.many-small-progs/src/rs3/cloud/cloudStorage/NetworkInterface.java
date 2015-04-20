package rs3.cloud.cloudStorage;

import rs3.cloud.lib.network.NetworkError;

/**
 * Interface to handle both test and real network implementations.
 */
public interface NetworkInterface{
	byte[] sendRequest(byte[] msgReq) throws NetworkError;
}
