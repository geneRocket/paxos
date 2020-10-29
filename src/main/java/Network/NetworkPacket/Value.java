package Network.NetworkPacket;

import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;

@Getter
@ToString
public class Value implements Serializable {
    UUID uuid;
    Queue<Object> queue;

    public Value(UUID uuid, Queue<Object> queue){
        this.uuid=uuid;
        this.queue=queue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Value value = (Value) o;
        return uuid.equals(value.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
