package core;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import Network.NetworkPacket.*;
import Network.NonBlockSend;
import Network.Send;
import conf.Node;
import conf.NodeSet;
import lombok.ToString;

public class Proposer {
    int proposer_id;
    NodeSet nodeSet;

    BlockingQueue<Packet> packet_queue = new LinkedBlockingQueue<>();
    Send send = new NonBlockSend();

    BlockingQueue<Object> wait_to_submit_value_queue = new LinkedBlockingQueue<>();
    Object submiting_value = null;
    Semaphore submit_success_semaphore = new Semaphore(0);

    Timer timer = new Timer();
    final int delay = 1000;

    final int start_ballot = 1;
    int current_instance = 0;

    HashMap<Integer, Instance> instance_record = new HashMap<>(); //instance idx -> instance

    public Proposer(int id, NodeSet nodeSet) throws IOException {
        this.proposer_id = id;
        this.nodeSet = nodeSet;

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

        new Thread(() -> {
            while (true) {
                try {
                    submiting_value = wait_to_submit_value_queue.take();
                    before_prepare();
                    submit_success_semaphore.acquire();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    enum Instance_State {
        init, prepare, accept, finish
    }

    @ToString
    static class Instance {
        Instance_State instance_state = Instance_State.init;

        int ballot;
        Set<Integer> prepare_acceptor_id_set = new HashSet<>();
        int accept_ballot = 0;
        Object value = null;
        Set<Integer> accept_acceptor_id_set = new HashSet<>();

        public Instance(int ballot) {
            this.ballot = ballot;
        }
    }


    void before_prepare() {
        this.current_instance++;
        Instance instance = new Instance(start_ballot);


        instance.instance_state = Instance_State.prepare;
        instance_record.put(this.current_instance, instance);
        prepare(current_instance);
    }

    void prepare(int instance_idx) {
        Instance instance = instance_record.get(instance_idx);

        PrepareRequest prepareRequest = new PrepareRequest();
        prepareRequest.setProposer_id(proposer_id);
        prepareRequest.setInstance(this.current_instance);
        prepareRequest.setBallot(instance.ballot);

        boardcast(Role.Acceptor, PacketType.PrepareRequest, prepareRequest);

        delay_exec(new TimerTask() {
            @Override
            public void run() {
                if (instance.instance_state == Instance_State.prepare) {
                    instance.ballot++;
                    instance.prepare_acceptor_id_set.clear();
                    prepare(instance_idx);
                }
            }
        });

    }

    void delay_exec(TimerTask timerTask) {
        timer.schedule(timerTask, delay);
    }

    void onPrepareResponse(PrepareResponse prepareResponse) {
        Instance instance = instance_record.get(prepareResponse.getInstance());
        if (instance.instance_state != Instance_State.prepare)
            return;
        if (instance.ballot != prepareResponse.getBallot()) {
            return;
        }


        if (prepareResponse.isOk()) {
            instance.prepare_acceptor_id_set.add(prepareResponse.getAcceptor_id());


            if (prepareResponse.getAccept_ballot() > instance.accept_ballot) {
                instance.accept_ballot = prepareResponse.getAccept_ballot();

                instance.instance_state = Instance_State.finish;
                before_prepare();
                return;
            }

            if (instance.prepare_acceptor_id_set.size() >= (nodeSet.getNodes().size() / 2 + 1)) {

                if (instance.accept_ballot == 0) {
                    accept(prepareResponse.getInstance());
                }
            }
        }

    }

    void accept(int instance_idx) {
        Instance instance = instance_record.get(instance_idx);
        instance.instance_state = Instance_State.accept;
        instance.value = submiting_value;
        assert instance.value != null;

        AcceptRequest acceptRequest = new AcceptRequest();
        acceptRequest.setProposer_id(proposer_id);
        acceptRequest.setInstance(instance_idx);
        acceptRequest.setBallot(instance.ballot);
        acceptRequest.setValue(instance.value);

        boardcast(Role.Acceptor, PacketType.AcceptRequest, acceptRequest);

        delay_exec(new TimerTask() {
            @Override
            public void run() {
                if (instance.instance_state == Instance_State.accept) {
                    instance.ballot++;
                    instance.prepare_acceptor_id_set.clear();
                    instance.accept_acceptor_id_set.clear();
                    prepare(instance_idx);
                }
            }
        });
    }

    void onAcceptResponse(AcceptResponse acceptResponse) {
        Instance instance = instance_record.get(acceptResponse.getInstance());
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

                System.out.println(acceptResponse);
                System.out.println(instance.value);

                submiting_value = null;
                submit_success_semaphore.release();


            }
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

    void handle_packet(Packet packet) {
        switch (packet.getType()) {
            case PrepareResponse:
                onPrepareResponse((PrepareResponse) packet.getData());
                break;

            case AcceptResponse:
                onAcceptResponse((AcceptResponse) packet.getData());
                break;

            case SubmitValue:
                BlockingQueue queue = (BlockingQueue) packet.getData();
                queue.drainTo(wait_to_submit_value_queue);
                break;
        }

    }

    void put_packet(Packet packet) {
        packet_queue.add(packet);
    }

}