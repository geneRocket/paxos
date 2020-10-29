package Network.NetworkPacket;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@ToString
public class PrepareResponse implements Serializable {
    int acceptor_id;
    int instance;
    int ballot;
    int accept_ballot;
    Value value;
    boolean ok;
}
