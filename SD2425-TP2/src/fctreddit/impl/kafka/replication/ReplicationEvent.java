package fctreddit.impl.kafka.replication;

public class ReplicationEvent {

    private long version;
    private String operation;
    private String key;
    private String value;

    public ReplicationEvent(long version, String operation, String key, String value) {
        this.version = version;
        this.operation = operation;
        this.key = key;
        this.value = value;
    }

    public String getOperation() {
        return operation;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return version + "|" + operation + "|" + key + "|" + value;
    }

    public static ReplicationEvent fromString(String line) {
        String[] parts = line.split("\\|", 4);
        return new ReplicationEvent(
                Long.parseLong(parts[0]),
                parts[1],
                parts[2],
                parts[3]
        );
    }

    public long getVersion(){
        return this.version;
    }
}
