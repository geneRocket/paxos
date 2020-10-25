package Network.NetworkPacket;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class Packet implements Serializable {
    Role receive_role;
    PacketType type;
    Object data;
}
