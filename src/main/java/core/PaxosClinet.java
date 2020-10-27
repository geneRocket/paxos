package core;

import Network.NetworkPacket.Packet;
import Network.NetworkPacket.PacketType;
import Network.NetworkPacket.Role;
import Network.NioSend;
import Network.Send;
import conf.Node;
import conf.NodeSet;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PaxosClinet {
    Send send;
    BlockingQueue<Object> msg_queue = new LinkedBlockingQueue<>();
    int BUFFER_SIZE=20;

    NodeSet nodeSet;


    public PaxosClinet() throws IOException {
        send=new NioSend();
        nodeSet=NodeSet.read_from_file("conf/nodes.json");
    }

    public void submit_value(Object object){
        msg_queue.add(object);
        if(msg_queue.size()>BUFFER_SIZE){
            flush();
        }
    }

    public void flush()  {
        if(msg_queue.size()==0)
            return;

        Packet packet =  new Packet();
        packet.setReceive_role(Role.Proposer);
        packet.setType(PacketType.SubmitValue);
        packet.setData(msg_queue);
        Node node=nodeSet.getNodes().get(0);

        try {
            send.send_to(node.getIp(),node.getPort(),packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        msg_queue.clear();
    }
}
