package network;

import java.util.Map;
import java.util.Objects;

public class UDPMessage {

    private String reading;

    private long scalarTime;

    private Map<String, Integer> vectorTime;

    public UDPMessage(String reading, long scalarTime, Map<String, Integer> vectorTime) {
        this.reading = reading;
        this.scalarTime = scalarTime;
        this.vectorTime = vectorTime;
    }

    public String getReading() {
        return reading;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

    public long getScalarTime() {
        return scalarTime;
    }

    public void setScalarTime(long scalarTime) {
        this.scalarTime = scalarTime;
    }

    public Map<String, Integer> getVectorTime() {
        return vectorTime;
    }

    public void setVectorTime(Map<String, Integer> vectorTime) {
        this.vectorTime = vectorTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UDPMessage that)) return false;
        return scalarTime == that.scalarTime && reading.equals(that.reading);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reading, scalarTime);
    }

    @Override
    public String toString() {
        return "[reading=" + reading +
                ", scalarTime=" + scalarTime +
                ", vectorTime=" + vectorTime +
                ']';
    }
}
