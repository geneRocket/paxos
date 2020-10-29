package Network.NetworkPacket;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class AcceptRequest implements Serializable {
    int proposer_id;
    int instance;
    int ballot;
    Value value;
}
