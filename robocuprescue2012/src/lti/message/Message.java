package lti.message;

import java.util.ArrayList;
import java.util.Collection;
import lti.message.Parameter.Operation;
import lti.message.type.Blockade;
import lti.message.type.BlockadeCleared;
import lti.message.type.BuildingBurnt;
import lti.message.type.Fire;
import lti.message.type.FireExtinguished;
import lti.message.type.TaskDrop;
import lti.message.type.TaskPickup;
import lti.message.type.Victim;
import lti.message.type.VictimDied;
import lti.message.type.VictimRescued;

public class Message {

	// Parameters
	private Collection<Parameter> params;

	// Message size in bytes
	private int size;

	/**
	 * Constructor
	 */
	public Message() {
		this.size = 0;
		this.params = new ArrayList<Parameter>();
	}

	/**
	 * Constructor
	 * 
	 * @param msg
	 *            Message
	 */
	public Message(byte[] msg) {
		this.params = new ArrayList<Parameter>();
		this.size = 0;

		Operation operation;
		int size;
		byte[] tmp;

		for (int i = 0; i < msg.length - 4; i++) {
			operation = Parameter.Operation.ofOperation(msg[i]);

			if (!operation.equals(Operation.NONE)) {
				size = operation.getSize() - 1;
				this.size += size;

				tmp = new byte[size];
				for (int j = 0; j < size; j++) {
					tmp[j] = msg[i + j + 1];
				}

				if (operation.equals(Operation.FIRE)) {
					this.params.add(new Fire(tmp));
				} else if (operation.equals(Operation.VICTIM)) {
					this.params.add(new Victim(tmp));
				} else if (operation.equals(Operation.BLOCKADE)) {
					this.params.add(new Blockade(tmp));
				} else if (operation.equals(Operation.TASK_PICKUP)) {
					this.params.add(new TaskPickup(tmp));
				} else if (operation.equals(Operation.TASK_DROP)) {
					this.params.add(new TaskDrop(tmp));
				} else if (operation.equals(Operation.BLOCKADE_CLEARED)) {
					this.params.add(new BlockadeCleared(tmp));
				} else if (operation.equals(Operation.VICTIM_DIED)) {
					this.params.add(new VictimDied(tmp));
				} else if (operation.equals(Operation.VICTIM_RESCUED)) {
					this.params.add(new VictimRescued(tmp));
				} else if (operation.equals(Operation.FIRE_EXTINGUISHED)) {
					this.params.add(new FireExtinguished(tmp));
				} else if (operation.equals(Operation.BUILDING_BURNT)) {
					this.params.add(new BuildingBurnt(tmp));
				}

				i += size;
			}
		}
	}

	/**
	 * Add a parameter into the message
	 * 
	 * @param param
	 *            Parameter
	 */
	public void addParameter(Parameter param) {
		this.params.add(param);
		this.size += param.getOperation().getSize();
	}

	/**
	 * Returns the message translated into bytes
	 * 
	 * @return Message
	 */
	public byte[] getMessage() {
		byte[] msg = new byte[this.size];

		int i = 0;
		byte[] tmp;
		for (Parameter param : this.params) {
			msg[i++] = param.getOperation().getByte();
			tmp = param.getByteAttributes();
			for (int j = 0; j < (param.getOperation().getSize() - 1); j++) {
				msg[i++] = tmp[j];
			}
		}

		return msg;
	}

	/**
	 * Get the list of parameters
	 * 
	 * @return
	 */
	public Collection<Parameter> getParameters() {
		return this.params;
	}

	/**
	 * 
	 */
	@Override
	public String toString() {
		String str = "[" + this.params.size() + "]";

		for (Parameter param : this.params) {
			str += " [" + param.toString() + "]";
		}

		return str;
	}
}