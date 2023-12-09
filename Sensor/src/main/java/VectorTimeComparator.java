import network.UDPMessage;

import java.util.Comparator;
import java.util.Map;

public class VectorTimeComparator implements Comparator<UDPMessage> {
    @Override
    public int compare(UDPMessage message1, UDPMessage message2) {
        Map<String, Integer> vectorTime1 = message1.getVectorTime();
        Map<String, Integer> vectorTime2 = message2.getVectorTime();

        for (Map.Entry<String, Integer> entry : vectorTime1.entrySet()) {
            Integer value1 = entry.getValue();        // value of current vectorTime
            Integer value2 = vectorTime2.get(entry.getKey());  // corresponding value in another vectorTime

            if (value1 < value2) {
                boolean allOtherValuesAreLessOrEqual = vectorTime1.entrySet().stream()
                        .filter(e -> !e.getKey().equals(entry.getKey()))
                        .allMatch(e -> vectorTime2.containsKey(e.getKey()) && e.getValue() <= vectorTime2.get(e.getKey()));
                if (allOtherValuesAreLessOrEqual) return -1; // vectorTime2 is greater
                else return 1; // vectorTime1 is greater
            } else if (value1 > value2) return 1;
        }
        return 0; // the same
    }
}
