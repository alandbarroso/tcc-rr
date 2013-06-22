package lti.message;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import lti.message.Parameter.Operation;
import lti.message.type.Blockade;
import lti.message.type.BlockadeCleared;
import lti.message.type.BuildingBurnt;
import lti.message.type.Fire;
import lti.message.type.FireExtinguished;
import lti.message.type.Help;
import lti.message.type.TaskDrop;
import lti.message.type.TaskPickup;
import lti.message.type.Victim;
import lti.message.type.VictimDied;
import lti.message.type.VictimRescued;

public class Message {

	private static final String MSG_OUCH_CIVILIAN = "Ouch";

	private static final String MSG_HELP_CIVILIAN = "Help";

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
		int size_attributes, size;
		byte[] tmp;

		String msg_text = "";
		try {
			msg_text = new String(msg, "UTF-8");
		} catch (UnsupportedEncodingException e) {}
		// Treat civilian ask for help differently because msg is different
		if (msg_text.equals(MSG_OUCH_CIVILIAN) ||
				msg_text.equals(MSG_HELP_CIVILIAN)) {
			operation = Parameter.Operation.ofOperation(Operation.HELP_CIVILIAN.getByte());
			this.size += operation.getSize();
			// Shouting in pain = DAMAGE > 0
			int is_damage = msg_text.equals(MSG_OUCH_CIVILIAN) ?  1 : 0;
			// Calling for help = BURIEDNESS > 0
			int is_buriedness = msg_text.equals(MSG_HELP_CIVILIAN) ? 1 : 0;
			this.params.add(new Help(is_damage, is_buriedness));
			return;
		}
		
		for (int i = 0; i < msg.length;) {
			operation = Parameter.Operation.ofOperation(msg[i]);

			if (operation.equals(Operation.NONE))
				break;
			
			size = operation.getSize();
			this.size += size;
			size_attributes = size - 1;

			tmp = new byte[size_attributes];
			for (int j = 0; j < size_attributes; j++) {
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
			for (int j = 0; j < tmp.length; j++) {
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