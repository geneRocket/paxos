package core;

import Network.NetworkPacket.*;


import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Acceptor {
    int acceptor_id;

    BlockingQueue<Packet> packet_queue = new LinkedBlockingQueue<>();
    NetUtil netUtil;

    static class Instance{
        int prepare_ballot;
        int accept_ballot=0;
        Value value=null;

        public Instance(int prepare_ballot){
            this.prepare_ballot = prepare_ballot;
        }
    }

    HashMap<Integer, Instance> instance_record = new HashMap<>(); //instance idx -> instance


    public Acceptor(int id,NetUtil netUtil) throws IOException {
        this.acceptor_id =id;
        this.netUtil=netUtil;
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
                //如果Acceptor已经接受过提案，那么就向Proposer响应已经接受过的编号小于N的最大编号的提案
                responsePrepare(prepareRequest.getProposer_id(),true,prepareRequest.getInstance(),instance.prepare_ballot,instance.accept_ballot);
            }
            else {
                //可忽略
                //responsePrepare(prepareRequest.getProposer_id(),false,prepareRequest.getInstance(),instance.prepare_ballot,instance.accept_ballot);
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
        this.netUtil.send_to(proposer_id,Role.Proposer,PacketType.PrepareResponse,prepareResponse);
    }

    void onAccept(AcceptRequest acceptRequest){
        //接受Accept请求的Acceptor集合不一定是之前响应Prepare请求的Acceptor集合
        if(!instance_record.containsKey(acceptRequest.getInstance())) {
            Instance instance=new Instance(acceptRequest.getBallot());
            instance_record.put(acceptRequest.getInstance(),instance);
        }

        Instance instance=instance_record.get(acceptRequest.getInstance());

        if(instance.prepare_ballot > acceptRequest.getBallot()){
            responseAccept(acceptRequest.getProposer_id(),acceptRequest.getInstance(),instance.prepare_ballot,false);
        }
        else {
            instance.value=acceptRequest.getValue();
            instance.prepare_ballot=acceptRequest.getBallot();
            instance.accept_ballot=acceptRequest.getBallot();

            //multi-paxos
            if(!instance_record.containsKey(acceptRequest.getInstance()+1)){
                Instance next_instance= new Instance(1);
                instance_record.put(acceptRequest.getInstance()+1,next_instance);
            }

            responseAccept(acceptRequest.getProposer_id(),acceptRequest.getInstance(),instance.prepare_ballot,true);
            sendLearnResponse(acceptor_id,acceptRequest.getInstance(),acceptRequest.getValue());
        }

    }

    void sendLearnResponse(int acceptor_id,int instance,Value value){
        LearnResponse learnResponse=new LearnResponse();
        learnResponse.setAcceptor_id(acceptor_id);
        learnResponse.setInstance(instance);
        learnResponse.setValue(value);
        this.netUtil.boardcast(Role.Learner,PacketType.LearnResponse,learnResponse);
    }

    void responseAccept(int proposer_id,int instance,int ballot,boolean is_scccess){
        AcceptResponse acceptResponse = new AcceptResponse();
        acceptResponse.setAcceptor_id(this.acceptor_id);
        acceptResponse.setInstance(instance);
        acceptResponse.setBallot(ballot);
        acceptResponse.setOk(is_scccess);

        this.netUtil.send_to(proposer_id,Role.Proposer,PacketType.AcceptResponse,acceptResponse);
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
