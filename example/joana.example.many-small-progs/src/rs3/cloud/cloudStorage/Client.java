package rs3.cloud.cloudStorage;

import java.util.Arrays;

import rs3.cloud.functionalities.nonce.NonceGen;
import rs3.cloud.functionalities.pkienc.Decryptor;
import rs3.cloud.functionalities.pkienc.Encryptor;
import rs3.cloud.functionalities.pkienc.RegisterEnc;
import rs3.cloud.functionalities.pkisig.RegisterSig;
import rs3.cloud.functionalities.pkisig.Signer;
import rs3.cloud.functionalities.pkisig.Verifier;
import rs3.cloud.functionalities.symenc.SymEnc;
import rs3.cloud.lib.network.NetworkError;
import rs3.cloud.utils.MessageTools;

public class Client {
	
	private SymEnc symenc;
	private Decryptor decryptor;
	private Signer signer;
	private Verifier verifier;
	private Encryptor server_enc;
	private Verifier server_ver;

	private int userID;
	private LabelList lastCounter;
	private NonceGen nonceGen;

	private NetworkInterface net;

	public Client(int userID, SymEnc symenc, Decryptor decryptor, Signer signer, NetworkInterface net) 
				  throws RegisterEnc.PKIError, RegisterSig.PKIError, NetworkError {
		this.symenc = symenc;
		this.decryptor = decryptor;
		this.signer = signer;
		this.verifier = signer.getVerifier();
		this.server_enc = RegisterEnc.getEncryptor(Params.SERVER_ID, Params.PKI_ENC_DOMAIN);
		this.server_ver = RegisterSig.getVerifier(Params.SERVER_ID, Params.PKI_DSIG_DOMAIN);
		this.userID = userID;
		this.net=net;
		
		lastCounter = new LabelList(); // for each label maintains the last counter
		nonceGen = new NonceGen();
	}


	/**
	 * Store a message on the server under a given label
	 */
	public void store(byte[] msg, byte[] label) throws NetworkError, StorageError {
		
		int serverLastCounter = getServerLastCounter(label);
		// pick the last the counter we stored
		int ourCounter = lastCounter.get(label); // note that if 'label' has not been used yet, lastCounter.get(label) returns -1
		
		if(serverLastCounter<ourCounter) // the server is misbehaving (his counter is expected to be higher)
			throw new IncorrectReply();
		else if(serverLastCounter>ourCounter){ // we aren't up to date with the current counter stored in the server
			lastCounter.put(label, serverLastCounter);
			throw new CounterOutOfDate();
		}
		// otherwise they are the same!
		int counter = ourCounter+1;
		
		// encrypt the message with the symmetric key (the secret key of the client) 
		byte[] encrMsg = symenc.encrypt(msg);
		
		// Encoding the message that has to be signed: (STORE, (label, (counter, encMsg)))
		byte[] counter_msg = MessageTools.concatenate(MessageTools.intToByteArray(counter), encrMsg);
		byte[] label_counter_msg = MessageTools.concatenate(label, counter_msg);
		byte[] store_label_counter_msg = MessageTools.concatenate(Params.STORE, label_counter_msg);

		
		/* HANDLE THE SERVER RESPONSE
		 * Expected server's responses (encrypted with the client's public key):
		 * 			((signClient, STORE_OK), signServer)							or
		 * 			((signClient, (STORE_FAIL, lastCounter)), signServer)
		 * where:
		 * - signServer: signature of all the previous tokens
		 * - signClient: signature of the message for which we are receiving the response 
		 * - lastCounter: the higher value of the counter associated with label, as stored by the server
		 */
		ServerResponse response = sendPayloadToServer(store_label_counter_msg);
		// response.info is either (STORE_OK, {}) or (STORE_FAIL, lastCounter)
			
		// analyze the response tag
		if(Arrays.equals(response.tag, Params.STORE_OK)){  // message successfully stored 
			// we can save the counter used to send the message
			lastCounter.put(label, counter);
			return;
		}
		else if(Arrays.equals(response.tag, Params.STORE_FAIL)){ // the server hasn't accepted the request, because it claims
				                                                     // to have a higher counter for this label
			byte[] serverCounter = response.info;
			if(serverCounter.length!=4) // since lastCounter is supposed to be a integer, its length must be 4 bytes
				throw new IncorrectReply();
			serverLastCounter = MessageTools.byteArrayToInt(serverCounter); 
			if (serverLastCounter<=counter) // the server is misbehaving (his counter is expected to be higher)
				throw new IncorrectReply();
			else if (serverLastCounter>counter){ // we aren't up to date with the current counter stored in the server
				lastCounter.put(label, serverLastCounter);
				throw new CounterOutOfDate();
			}
		}
		else
			throw new IncorrectReply();
		
		// FIXME: Don't we have StoreFailure() exception anymore?
		// throw new StoreFailure(); 
		// This exception may be thrown if several clients try to store 
		// concurrently a message into the server with the same label.
	}

	/**
	 * Retrieve from the server the message related to the correspondent label
	 *  
	 * @param label the label related to the message to be retrieved
	 * @return the message in the server related to the label if it exists, null otherwise
	 */
	public byte[] retrieve(byte[] label) throws NetworkError, StorageError {
		
		int counter = getServerLastCounter(label);
		
		// pick the last the counter we stored
		int ourCounter = lastCounter.get(label); // note that if 'label' has not been used yet, lastCounter.get(label) returns -1
		
		if(counter<ourCounter) // the server is misbehaving (his counter is expected to be higher)
			throw new IncorrectReply();
		
		if(counter<0) // if counter<0 now we are sure that the server doesn't have anything under this label
			return null;

		// create the message to send
		byte[] label_counter = MessageTools.concatenate(label, MessageTools.intToByteArray(counter));
		
		byte[] retrieve_label_counter = MessageTools.concatenate(Params.RETRIEVE, label_counter);

		/* HANDLE THE SERVER RESPONSE
		 * Expected server's responses (encrypted with the client's public key):
		 * 			((signClient, (RETRIEVE_OK, (encMsg, signEncrMsg))), signServer)					or
		 * 			((signClient, RETRIEVE_FAIL, {}), signServer)
		 * where:
		 * - signServer: signature of all the previous tokens
		 * - signClient: signature of the message for which we are receiving the response 
		 * - signEncMsg: the signature of ((STORE, (label, (counter, encrMsg)))
		 */
		ServerResponse response = sendPayloadToServer(retrieve_label_counter);
		// response.inf is either (RETRIEVE_OK, (encMsg, signEncrMsg)) or (RETRIEVE_FAIL,{})
		
		// analyze the response tag
		if(Arrays.equals(response.tag, Params.RETRIEVE_OK)){
			byte[] encrMsg = MessageTools.first(response.info);
			byte[] signMsg = MessageTools.second(response.info);
			// check whether the signMsg is the signature for the STORE request with encrMsg
			// which is of the form (STORE, (label, (counter, encrMsg)))
			byte[] counter_msg = MessageTools.concatenate(MessageTools.intToByteArray(counter), encrMsg);
			byte[] label_counter_msg = MessageTools.concatenate(label, counter_msg);
			byte[] store_label_counter_msg = MessageTools.concatenate(Params.STORE, label_counter_msg);
			if(!verifier.verify(signMsg, store_label_counter_msg))  // the server hasn't replied with the encrypted message we requested
				throw new IncorrectReply();
			// everything is ok; decrypt the message and return it 
			return symenc.decrypt(encrMsg);
		}
		else if(Arrays.equals(response.tag, Params.RETRIEVE_FAIL)){
			// The server claims that it counldn't retrieve the message.
			// But because the 'counter' is saved only after the server acknowledges 
			// that the message was successfully stored, it should not happen.
			throw new IncorrectReply();
		}
		else
			throw new MalformedMessage();
	}
	
	
	private class ServerResponse {
		byte[] tag;
		byte[] info;
		
		ServerResponse(byte[] tag, byte[] info) {
			this.tag = tag;
			this.info = info;
		}
	}
	
	/**
	 * Retrieve from the server the highest counter related to (clientID, label)
	 * If there isn't any counter related to this pair, return -1  
	 * 
	 */
	private int getServerLastCounter(byte[] label) throws NetworkError, StorageError {
		// pick a nonce
		byte[] nonce = nonceGen.newNonce();
		byte[] label_nonce=MessageTools.concatenate(label, nonce);
		byte[] store_label_nonce=MessageTools.concatenate(Params.GET_COUNTER, label_nonce);
				
		ServerResponse response = sendPayloadToServer(store_label_nonce);
				
		// analyze the response tag
		if(!Arrays.equals(response.tag, Params.LAST_COUNTER))
			throw new MalformedMessage();
		byte[] lastCounter_nonceResp = response.info;
		byte[] lastCounter = MessageTools.first(lastCounter_nonceResp);
		byte[] nonceResp = MessageTools.second(lastCounter_nonceResp);
		if(!Arrays.equals(nonce, nonceResp))
			throw new IncorrectReply();
		if(lastCounter.length!=4)
			throw new MalformedMessage();
		return MessageTools.byteArrayToInt(lastCounter);
	}
	
	/**
	 * Sign the payload, add userID, encrypt with PKE and send everything to the server.
	 * Decrypt and validate the server response.  
	 */
	private ServerResponse sendPayloadToServer(byte[] payload) throws MalformedMessage, NetworkError{
		// sign the message with the client private key 
		byte[] signClient = signer.sign(payload);
		byte[] msgWithSignature = MessageTools.concatenate(payload, signClient);

		// encrypt the (userID, ([payload], clientSign)) with the server public key
		byte[] msgToSend = server_enc.encrypt(MessageTools.concatenate(MessageTools.intToByteArray(userID), msgWithSignature));			
		// Shape of msgToSend:
		//		(userID, ([payload], signClient))
		// where signClient is the signature of [payload]

		// send the message to the server
		byte[] encryptedSignedResp = net.sendRequest(msgToSend);
		// Decrypt the validate the message in order to make sure that it is a response to the client's request.
		return decryptValidateResp(encryptedSignedResp, signClient);
	}
	
	/**
	 * Decrypt the message, verify that it's a response of the server to our request
	 * (otherwise an exception is thrown). 
	 * 
	 * @param encryptedSignedResponse the message received from the network. Its shape should be: Enc_Client{((signClient, msgCore), signServer)}
	 * @param signRequest the signature on the client's request
	 * @return a ServerResponse object
	 * @throws MalformedMessage if something went wrong during the validation process
	 */
	private ServerResponse decryptValidateResp(byte[] encryptedSignedResponse, byte[] signRequest) throws MalformedMessage {
		// decrypt the message with the client private key and parse it
		byte[] signedResponse = decryptor.decrypt(encryptedSignedResponse);
		byte[] payload = MessageTools.first(signedResponse);
		byte[] signServer = MessageTools.second(signedResponse);

		// if the signature isn't correct, the message is malformed
		// (note that the signature is incorrect even if one or both messages are empty)
		if (!server_ver.verify(signServer, payload))
			throw new MalformedMessage();
		// check whether this is a response to the client's request as identified by signRequest 
		byte[] signatureClient = MessageTools.first(payload);
		if(!Arrays.equals(signatureClient, signRequest))
			throw new MalformedMessage();
		byte[] response = MessageTools.second(payload); // response should be of the form (tag, info), where info may be empty
		return new ServerResponse(MessageTools.first(response), MessageTools.second(response)); 
	}

	public class StorageError extends Exception {}
	
	/**
	 * Exception thrown when the response is invalid and demonstrates that the server
	 * has misbehaved (the server has be ill-implemented or malicious).
	 */
	public class IncorrectReply extends StorageError {}

	/**
	 * Exception thrown when the response of the server does not conform
	 * to an expected format (we get, for instance, a trash message or a response
	 * to a different request). 
	 */
	public class MalformedMessage extends StorageError {}
	
	
	/**
	 * Exception thrown when the server is not able to store the message we sent to it, e.g.
	 * because it has always an higher counter related to our label.
	 */
	public class StoreFailure extends StorageError {}
	
	/**
	 * Exception thrown when the lastCounter provided by the server is higher than our counter.
	 * Before throwing this exception we should update our counter to the server one.
	 */
	public class CounterOutOfDate extends StorageError{}
	
	
	/**
	 * List of labels.
	 * For each 'label' maintains an counter representing 
	 * how many times the label has been used.
	 */
	static private class LabelList {

		static class Node {
			byte[] key;
			int counter;
			Node next;

			public Node(byte[] key, int counter, Node next) {
				this.key = key;
				this.counter = counter;
				this.next = next;
			}
		}

		private Node firstElement = null;

		public void put(byte[] key, int counter) {
			for(Node tmp = firstElement; tmp != null; tmp=tmp.next)
				if( Arrays.equals(key, tmp.key) ){
					tmp.counter = counter;
					return;
				}
			firstElement = new Node(key, counter, firstElement);
		}

		public int get(byte[] key) {
			for(Node tmp = firstElement; tmp != null; tmp=tmp.next)
				if( Arrays.equals(key, tmp.key)  )
					return tmp.counter;	
			return -1; // if the label is not present, return -1
		}
	}
}
