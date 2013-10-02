package mrl.communication.property;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 3, 2011
 * Time: 4:41:30 PM
 */
public class MessageProperty {
    protected PropertyName propertyName;
    protected int value;
    protected int byteNeeded;

    public MessageProperty(PropertyName propertyName, int byteNeeded) {
        this.propertyName = propertyName;
        this.byteNeeded = byteNeeded;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public PropertyName getPropertyName() {
        return propertyName;
    }

    public int getValue() {
        return value;
    }

    public int getByteNeeded() {
        return byteNeeded;
    }

}
