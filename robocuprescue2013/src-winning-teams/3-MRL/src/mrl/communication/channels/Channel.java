package mrl.communication.channels;

import javolution.util.FastMap;
import mrl.communication.property.Priority;

import java.util.Map;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 6, 2011
 * Time: 11:30:58 AM
 */
public class Channel implements Comparable {
    int id;
    int bandwidth;
    int averageBandwidth;
    int realBandwidth;
    int remainedBandWidth;
    String type;
    double dropout;
    double failure;
    boolean scanned;
    boolean sendMessage;
    Map<Priority, Integer> repeatContMap;

    public Channel(int id, int bandwidth, String type) {
        this.id = id;
        this.bandwidth = bandwidth;
        this.realBandwidth = bandwidth;
        this.type = type;
        scanned = false;
        sendMessage = false;
        repeatContMap = new FastMap<Priority, Integer>();
    }

    public void setPrimaryChannel(int agentSize) {
        setAverageBandwidth(agentSize / 2);

        for (Priority priority : Priority.values()) {
            setRepeatCont(priority, 1);
        }
    }

    public void setBreakConditionAverageChannel(int agentSize) {
        setAverageBandwidth(agentSize / 2);

        for (Priority priority : Priority.values()) {
            setRepeatCont(priority, 2);
        }
    }

    public void setAverageBandwidth(int agentInThisChannel) {
        if (bandwidth < 250) {
            if (agentInThisChannel >= 10) {
                averageBandwidth = 21;
                resetRemainedBandWidth();
                return;
            }
        }
        if (agentInThisChannel == 0) {
            agentInThisChannel = 1;
        }
        this.averageBandwidth = bandwidth / agentInThisChannel;
        resetRemainedBandWidth();
    }

    public void setRealBandwidth(int realBandwidth) {
        this.realBandwidth = realBandwidth;
    }

    public void resetRemainedBandWidth() {
        remainedBandWidth = averageBandwidth;
    }

    public void setDropout(double dropout) {
        this.dropout = dropout;
    }

    public void setFailure(double failure) {
        this.failure = failure;
    }

    public void setScanned() {
        scanned = true;
    }

    public void setSendMessage() {
        this.sendMessage = true;
    }

    public void setRepeatCont(Priority priority, int repeatCont) {
        this.repeatContMap.put(priority, repeatCont);
    }

    public int getId() {
        return id;
    }

    public Integer getBandwidth() {
        return bandwidth;
    }

    public int getRealBandwidth() {
        return realBandwidth;
    }

    public int getRemainedBandWidth() {
        return remainedBandWidth;
    }

    public String getType() {
        return type;
    }

    public boolean isScanned() {
        return scanned;
    }

    public boolean isSendMessage() {
        return sendMessage;
    }

    public int getRepeatCont(Priority priority) {
        Integer repeat = repeatContMap.get(priority);
        if (repeat == null) {
            return 2;
        } else {
            return repeat;
        }
    }

    public void calculateRealBandwidthAndRepeatCont() {
        double exclusion;
        float noise;
        float[] data;

        exclusion = (failure * (double) bandwidth) + (dropout * (double) bandwidth);
        setRealBandwidth((bandwidth - (int) Math.floor(exclusion)));

        noise = (float) (dropout + failure);
        for (Priority priority : Priority.values()) {

            data = new float[]{priority.ordinal() + 1, realBandwidth, noise};
            setRepeatCont(priority, FuzzyMessageRepeat.getFuzzyValue(data));
        }
    }

    public void decreaseRemainedBW(int dec) {
        remainedBandWidth -= dec;
    }

    @Override
    public String toString() {
        return "Channel[id:" + id + ", type:" + type + ", BW:" + bandwidth + ", failure:" + failure + ", dropout:" + dropout + ", realBW:" + realBandwidth + "]";
    }

    @Override
    public int compareTo(Object o) {
        int b1 = realBandwidth;
        int b2 = ((Channel) o).getRealBandwidth();
        if (b1 < b2)
            return 1;
        if (b1 == b2)
            return 0;

        return -1;
    }
}
