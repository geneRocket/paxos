package echo;

import core.PaxosServer;

import java.io.IOException;

public class Server1 {
    public static void main(String[] args) throws IOException {
        PaxosServer paxosServer=new PaxosServer(0,new EchoCallback());
        paxosServer.start();
    }
}
