package core;

import Network.NetworkPacket.*;
import Network.NioSend;
import Network.Send;
import conf.Node;
import conf.NodeSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Learner {
    int learner_id;
    NodeSet nodeSet;

    BlockingQueue<Packet> packet_queue = new LinkedBlockingQueue<>();
    Send send = new NioSend();

    Acceptor acceptor;
    StateMachineCallback stateMachineCallback;

    HashMap<Integer,HashMap<Object,Integer>> instance_value_count = new HashMap<>();

    HashMap<Integer,Object> accpted_values = new HashMap<>();

    int current_instance=1;

    public Learner(int id, NodeSet nodeSet,Acceptor acceptor,StateMachineCallback stateMachineCallback) throws IOException {
        this.learner_id = id;
        this.nodeSet = nodeSet;
        this.acceptor=acceptor;
        this.stateMachineCallback=stateMachineCallback;

        new Thread(() -> {
            while (true) {
                try {
                    Packet packet = packet_queue.take();
                    handle_packet(packet);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }


    void onLearnRequest(LearnRequest learnRequest){
        if(acceptor.instance_record.containsKey(learnRequest.getInstance())
            && acceptor.instance_record.get(learnRequest.getInstance()).value!=null){
            LearnResponse learnResponse=new LearnResponse();
            learnResponse.setValue(acceptor.instance_record.get(learnRequest.getInstance()).value);
            learnResponse.setInstance(learnRequest.getInstance());
            learnResponse.setAcceptor_id(acceptor.acceptor_id);

            Node node=nodeSet.getNodes().get(learnRequest.getLearner_id());
            try {
                send.send_to(node.getIp(),node.getPort(),learnResponse);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void onLearnResponse(LearnResponse learnResponse){
        if(accpted_values.containsKey(learnResponse.getInstance()))
            return;
        if(!instance_value_count.containsKey(learnResponse.getInstance())){
            instance_value_count.put(learnResponse.getInstance(),new HashMap<>());
        }
        HashMap<Object,Integer> value_count= instance_value_count.get(learnResponse.getInstance());
        Object value=learnResponse.getValue();
        if(!value_count.containsKey(value)){
            value_count.put(value,0);
        }
        value_count.put(value,value_count.get(value)+1);

        if(value_count.get(value)>=(nodeSet.getNodes().size()/2+1)){
            accpted_values.put(learnResponse.getInstance(),value);
            if(current_instance==learnResponse.getInstance()){
                current_instance++;
                stateMachineCallback.callback(value);
            }
        }
    }

    void handle_packet(Packet packet) {
        switch (packet.getType()) {
            case LearnRequest:
                onLearnRequest((LearnRequest) packet.getData());
                break;

            case LearnResponse:
                onLearnResponse((LearnResponse) packet.getData());
                break;


        }

    }

    void put_packet(Packet packet){
        packet_queue.add(packet);
    }
}
