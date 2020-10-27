package Network.NetworkPacket;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class LearnResponse implements Serializable {
    int acceptor_id;
    int instance;
    Object value;
}
