package mrl.common.BenchMark.messaging;

/**
 * @author Mahdi
 */
public enum CommunicationType {
    Speak,
    Say,
    Emergency;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
