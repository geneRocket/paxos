package core;

import Network.NetworkPacket.Packet;
import Network.NetworkPacket.Role;
import Network.NioRecv;
import Network.Recv;
import conf.Node;
import conf.NodeSet;

import java.io.IOException;

public class PaxosServer {
    int id;
    private NodeSet nodeSet;
    private Proposer proposer;
    private Acceptor acceptor;
    private Recv recv;
    private StateMachineCallback stateMachineCallback;

    public PaxosServer(int id,StateMachineCallback stateMachineCallback) throws IOException {
        this.id=id;
        nodeSet=NodeSet.read_from_file("conf/nodes.json");
        proposer = new Proposer(id,nodeSet);
        acceptor = new Acceptor(id,nodeSet);
        Node node=nodeSet.getNodes().get(id);
        recv=new NioRecv(node.getIp(),node.getPort());
        this.stateMachineCallback=stateMachineCallback;
    }

    public void start(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    Object object= null;
                    try {
                        object = recv.receive_object();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        break;
                    }
                    Packet packet=(Packet) object;
                    assert packet!=null;
                    switch (packet.getReceive_role()){
                        case Proposer:
                            proposer.put_packet(packet);
                            break;
                        case Acceptor:
                            acceptor.put_packet(packet);
                            break;
                        default:
                            System.out.println("receive role error:"+packet.getReceive_role());
                    }
                }
            }
        }).start();
    }
    
}
