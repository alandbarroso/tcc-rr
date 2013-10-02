package Communication;

public class VoiceMessage {

	/*
	 * The data transmitted in the voice message
	 */
	private byte[] data;
	/*
	 * The priority of the voice message (1 highest priority)
	 */
	private int priority;
	/*
	 * The time to live of the voice message indicating the number of times to
	 * be propagated
	 */
	private int timeToLive;
	/*
	 * The type of the voice message
	 */
	private String messageType;

	/**
	 * Constructor for the voice message
	 * 
	 * @param data
	 *            : the data to be transmitted in the voice message
	 * @param priority
	 *            : the priority of the voice message
	 * @param timeToLive
	 *            : the time to live of the voice message
	 * @param messageType
	 *            : the type (id) of the voice message
	 */
	public VoiceMessage(byte[] data, int priority, int timeToLive,
			String messageType) {
		this.data = new byte[data.length];
		for (int i = 0; i < data.length; i++) {
			this.data[i] = data[i];
		}
		this.priority = priority;
		this.timeToLive = timeToLive;
		this.messageType = messageType;
	}

	/**
	 * Setter for the data to be transmitted in the voice message
	 * 
	 * @param data
	 *            : the data to be transmitted in the voice message
	 */
	public void setData(byte[] data) {
		this.data = new byte[data.length];
		for (int i = 0; i < data.length; i++) {
			this.data[i] = data[i];
		}
	}

	/**
	 * Setter for the voice message priority
	 * 
	 * @param priority
	 *            : the priority of the voice message
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * Setter for the time to live of the voice message
	 * 
	 * @param time
	 *            : the time to live of the voice message
	 */
	public void setTTL(int time) {
		this.timeToLive = time;
	}

	/**
	 * Setter for the voice message type
	 * 
	 * @param msg
	 *            : the type (id) of the voice message
	 */
	public void setMessageType(String msg) {
		this.messageType = msg;
	}

	/**
	 * Getter for the type of the voice message
	 * 
	 * @return
	 */
	public String getMessageType() {
		return this.messageType;
	}

	/**
	 * Getter for the data to be transmitted through the voice message
	 * 
	 * @return
	 */
	public byte[] getData() {
		return this.data;
	}

	/**
	 * Getter for the priority of the voice message
	 * 
	 * @return
	 */
	public int getPriority() {
		return this.priority;
	}

	/**
	 * Getter for the time to live of the voice message
	 * 
	 * @return
	 */
	public int getTimeToLive() {
		return this.timeToLive;
	}

	/**
	 * Decrements the time to live of the voice message by 1
	 */
	public void decrementTTL() {
		this.timeToLive--;
	}

}
