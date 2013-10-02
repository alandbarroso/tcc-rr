package mrl.common.BenchMark.messaging;

/**
 * @author Mahdi
 */
public enum  TransmitType {
    SEND,
    RECEIVE, EMERGENCY_SEND;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
