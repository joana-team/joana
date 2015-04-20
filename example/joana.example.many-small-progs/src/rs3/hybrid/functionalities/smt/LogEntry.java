package rs3.hybrid.functionalities.smt;

class LogEntry {
	final byte[] message;
	final int sender_id;
	final int receiver_id;

	LogEntry(byte[] message, int sender_id, int receiver_id) {
		this.message = message;
		this.sender_id = sender_id;
		this.receiver_id = receiver_id;
	}
}