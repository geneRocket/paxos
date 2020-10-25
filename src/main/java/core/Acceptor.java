package core;

import Network.NetworkPacket.*;
import Network.NonBlockSend;
import Network.Send;
import conf.Node;
import conf.NodeSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Acceptor {
    int acceptor_id;
    NodeSet nodeSet;

    BlockingQueue<Packet> packet_queue = new LinkedBlockingQueue<>();
    Send send= new NonBlockSend();


    static class Instance{
        int prepare_ballot;
        int accept_ballot =0;
        Object value=null;

        public Instance(int prepare_ballot){
            this.prepare_ballot = prepare_ballot;
        }
    }

    HashMap<Integer, Instance> instance_record = new HashMap<>(); //instance idx -> instance


    public Acceptor(int id,NodeSet nodeSet) throws IOException {
        this.acceptor_id =id;
        this.nodeSet=nodeSet;

        new Thread(()->{
            while (true){
                try {
                    Packet packet = packet_queue.take();
                    handle_packet(packet);
                } catch (InterruptedException e) {
                    break;
                }
            }

        }).start();
    }

    void onPrepare(PrepareRequest prepareRequest){
        if(!instance_record.containsKey(prepareRequest.getInstance())){
            Instance instance= new Instance(prepareRequest.getBallot());
            instance_record.put(prepareRequest.getInstance(),instance);
            responsePrepare(prepareRequest.getProposer_id(),true,prepareRequest.getInstance(),prepareRequest.getBallot(),instance.accept_ballot);
        }
        else {
            Instance instance=instance_record.get(prepareRequest.getInstance());

            if(instance.prepare_ballot < prepareRequest.getBallot()){
                instance.prepare_ballot = prepareRequest.getBallot();
                responsePrepare(prepareRequest.getProposer_id(),true,prepareRequest.getInstance(),instance.prepare_ballot,instance.accept_ballot);
            }
            else {
                //可忽略
                responsePrepare(prepareRequest.getProposer_id(),false,prepareRequest.getInstance(),instance.prepare_ballot,instance.accept_ballot);
            }
        }
    }

    void responsePrepare(int proposer_id,boolean is_success,int instance,int ballot,int accept_ballot){
        PrepareResponse prepareResponse = new PrepareResponse();
        prepareResponse.setAcceptor_id(this.acceptor_id);
        prepareResponse.setOk(is_success);
        prepareResponse.setInstance(instance);
        prepareResponse.setBallot(ballot);
        prepareResponse.setAccept_ballot(accept_ballot);
        send_to(proposer_id,Role.Proposer,PacketType.PrepareResponse,prepareResponse);
    }

    void onAccept(AcceptRequest acceptRequest){
        if(instance_record.containsKey(acceptRequest.getInstance())){
            Instance instance=instance_record.get(acceptRequest.getInstance());

            if(instance.prepare_ballot != acceptRequest.getBallot()){
                responseAccept(acceptRequest.getProposer_id(),acceptRequest.getInstance(),instance.prepare_ballot,false);
            }
            else {
                instance.value=acceptRequest.getValue();
                responseAccept(acceptRequest.getProposer_id(),acceptRequest.getInstance(),instance.prepare_ballot,true);
            }
        }
    }

    void responseAccept(int proposer_id,int instance,int ballot,boolean is_scccess){
        AcceptResponse acceptResponse = new AcceptResponse();
        acceptResponse.setAcceptor_id(this.acceptor_id);
        acceptResponse.setInstance(instance);
        acceptResponse.setBallot(ballot);
        acceptResponse.setOk(is_scccess);

        send_to(proposer_id,Role.Proposer,PacketType.AcceptResponse,acceptResponse);
    }



    void send_to(int id, Role receive_role,PacketType type, Object data){
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



    void handle_packet(Packet packet){
        switch (packet.getType()){
            case PrepareRequest:
                onPrepare((PrepareRequest) packet.getData());
                break;

            case AcceptRequest:
                onAccept((AcceptRequest)packet.getData());
                break;
        }

    }

    void put_packet(Packet packet){
        packet_queue.add(packet);
    }

}
