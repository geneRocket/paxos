package core;

import Network.NetworkPacket.Packet;
import Network.NetworkPacket.PacketType;
import Network.NetworkPacket.Role;
import Network.Send;
import conf.Node;
import conf.NodeSet;

import java.io.IOException;

public class NetUtil {
    NodeSet nodeSet;
    Send send;

    public NetUtil(NodeSet nodeSet,Send send){
        this.nodeSet=nodeSet;
        this.send=send;
    }

    void send_to(int id, Role receive_role, PacketType type, Object data){
        Packet packet=new Packet();
        packet.setReceive_role(receive_role);
        packet.setType(type);
        packet.setData(data);

        Node node=nodeSet.getNodes().get(id);
        try {
            send.send_to(node.getIp(),node.getPort(),packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void boardcast(Role receive_role, PacketType type, Object data) {
        Packet packet = new Packet();
        packet.setReceive_role(receive_role);
        packet.setType(type);
        packet.setData(data);

        int n_nodes = nodeSet.getNodes().size();
        for (int i = 0; i < n_nodes; i++) {
            Node node = nodeSet.getNodes().get(i);
            try {
                send.send_to(node.getIp(), node.getPort(), packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
