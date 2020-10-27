package echo;

import core.PaxosServer;

import java.io.IOException;

public class Server2 {
    public static void main(String[] args) throws IOException {
        PaxosServer paxosServer=new PaxosServer(1,new EchoCallback());
        paxosServer.start();
    }
}
