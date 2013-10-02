package mrl.mosCommunication.message.property;

import mrl.mosCommunication.message.IDConverter;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 12:54 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
public class RoadIndexProperty extends AbstractProperty {
    public RoadIndexProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(IDConverter.getRoadsBitSize());
    }
}
