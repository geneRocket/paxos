package core;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import Network.NetworkPacket.*;
import conf.NodeSet;
import lombok.ToString;


public class Proposer {
    int proposer_id;
    NodeSet nodeSet;

    BlockingQueue<Packet> packet_queue = new LinkedBlockingQueue<>();

    NetUtil netUtil;

    BlockingQueue<Value> wait_to_submit_value_queue = new LinkedBlockingQueue<>();
    Value submiting_value = null;

    static final int delay = 2000;
    static final int default_delay=9999999;
    DelayExecRecord delayExecRecord=new DelayExecRecord(default_delay);

    int current_instance = 0;
    Instance instance= new Instance();

    boolean is_last_success_accept=false;

    public Proposer(int id, NodeSet nodeSet,NetUtil netUtil) throws IOException {
        this.proposer_id = id;
        this.nodeSet = nodeSet;
        this.netUtil=netUtil;

        new Thread(() -> {
            while (true) {
                long time_left=delayExecRecord.get_next_time_left();
                if(time_left<=0){
                    DelayExecRecord.TimeObject timeObject = delayExecRecord.take_soonest_delay_exec();
                    if(timeObject!=null){
                        switch (timeObject.eventType){
                            case PrepareCheck:
                                prepare_check(timeObject.instance_id);
                                break;
                            case AcceptCheck:
                                accept_delay(timeObject.instance_id);
                                break;
                            default:
                                System.err.println("event Type error");
                        }
                    }
                }

                if(submiting_value==null && wait_to_submit_value_queue.size()>0){
                    try {
                        submiting_value = wait_to_submit_value_queue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    new_instance();
                }

                time_left=delayExecRecord.get_next_time_left();
                Packet packet = null;
                try {
                    packet = packet_queue.poll(time_left, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(packet!=null)
                    handle_packet(packet);
            }
        }).start();

    }

    enum Instance_State {
        init, prepare, accept, finish
    }

    @ToString
    static class Instance {
        static final int start_ballot = 1;

        Instance_State instance_state;

        int ballot;
        Set<Integer> prepare_acceptor_id_set = new HashSet<>();

        int accept_ballot;
        Value value;
        Set<Integer> accept_acceptor_id_set = new HashSet<>();

        boolean is_new_value;

        public Instance() {
            reset();
        }

        public void reset(){
            instance_state = Instance_State.init;
            ballot=start_ballot;
            prepare_acceptor_id_set.clear();
            accept_ballot = 0;
            value=null;
            accept_acceptor_id_set.clear();
            is_new_value=false;
        }
    }


    void new_instance() {
        this.current_instance++;
        instance.reset();
        instance.instance_state = Instance_State.prepare;
        if(!is_last_success_accept)
            prepare();
        else {
            accept();
        }
    }

    void prepare() {
        PrepareRequest prepareRequest = new PrepareRequest();
        prepareRequest.setProposer_id(proposer_id);
        prepareRequest.setInstance(this.current_instance);
        prepareRequest.setBallot(instance.ballot);

        this.netUtil.boardcast(Role.Acceptor, PacketType.PrepareRequest, prepareRequest);

        delay_exec(DelayExecRecord.EventType.PrepareCheck,current_instance);
    }

    void prepare_check(int instance_idx){
        //prepare
        if(instance_idx!=current_instance)
            return;

        if (instance.instance_state == Instance_State.prepare) {
            instance.ballot++;
            instance.prepare_acceptor_id_set.clear();
            prepare();
        }
    }

    void onPrepareResponse(PrepareResponse prepareResponse) {
        if(prepareResponse.getInstance()!=current_instance)
            return;
        if (instance.instance_state != Instance_State.prepare)
            return;
        if (instance.ballot != prepareResponse.getBallot()) {
            return;
        }
        if (prepareResponse.isOk()) {
            instance.prepare_acceptor_id_set.add(prepareResponse.getAcceptor_id());

            //V是所有的响应中编号最大的提案的Value。如果所有的响应中都没有提案，那么此时V就可以由Proposer自己选择。
            //这里不能立即结束，尽管已经确定了值，还要提交这个值
            //1.因为可能proposer以为之前ballot没提交成功（prepareresponse丢失或超时），实际上大多数Acceptor已经accept,如果这里立即结束开始下一个instance，就会造成下一个instance重复提交这个value
            //2.还有可能value只被少部分acceptor接收，实际上没有成功提交，所以需要重复提交一次

            //为什么要取编号最大的value
            //可能有一些value只被少部分acceptor接收，后面有一些更大编号的value prepare的时候没经过这些少部分acceptor，所以选了新的值。
            if (prepareResponse.getAccept_ballot() > instance.accept_ballot) {
                instance.accept_ballot = prepareResponse.getAccept_ballot();
                instance.value=prepareResponse.getValue();
            }

            if (instance.prepare_acceptor_id_set.size() >= (nodeSet.getNodes().size() / 2 + 1)) {
                accept();
            }
        }

    }

    void accept() {
        instance.instance_state = Instance_State.accept;

        if(instance.value==null){
            assert submiting_value != null;
            instance.value = submiting_value;
            instance.is_new_value=true;
        }

        AcceptRequest acceptRequest = new AcceptRequest();
        acceptRequest.setProposer_id(proposer_id);
        acceptRequest.setInstance(current_instance);
        acceptRequest.setBallot(instance.ballot);
        acceptRequest.setValue(instance.value);

        this.netUtil.boardcast(Role.Acceptor, PacketType.AcceptRequest, acceptRequest);

        delay_exec(DelayExecRecord.EventType.AcceptCheck,current_instance);
    }

    void accept_delay(int instance_idx){
        if(instance_idx!=current_instance)
            return;

        if (instance.instance_state == Instance_State.accept) {
            instance.ballot++;
            instance.prepare_acceptor_id_set.clear();
            instance.accept_acceptor_id_set.clear();
            prepare();
        }
    }

    void onAcceptResponse(AcceptResponse acceptResponse) {
        if(acceptResponse.getInstance()!=current_instance)
            return;
        if (instance.instance_state != Instance_State.accept) {
            return;
        }
        if (instance.ballot != acceptResponse.getBallot()) {
            return;
        }
        if (acceptResponse.isOk()) {
            instance.accept_acceptor_id_set.add(acceptResponse.getAcceptor_id());
            if (instance.accept_acceptor_id_set.size() >= (nodeSet.getNodes().size() / 2 + 1)) {
                instance.instance_state = Instance_State.finish;

                //可能之前提交只有少部分acceptor接收了，或者acceptResponse丢失了，所以不一定是new_value
                if(submiting_value.equals(instance.value)){
                    submiting_value = null;
                }

                is_last_success_accept= instance.is_new_value;
            }
        }
    }

    void delay_exec(DelayExecRecord.EventType eventType, int instance_id) {
        delayExecRecord.add_time_record(delay, eventType,instance_id);
    }


    void handle_packet(Packet packet) {
        System.out.println(packet);
        switch (packet.getType()) {
            case PrepareResponse:
                onPrepareResponse((PrepareResponse) packet.getData());
                break;

            case AcceptResponse:
                onAcceptResponse((AcceptResponse) packet.getData());
                break;

            case SubmitValue:
                Value value = (Value) packet.getData();
                wait_to_submit_value_queue.add(value);
                break;
        }

    }

    void put_packet(Packet packet) {
        packet_queue.add(packet);
    }

}
