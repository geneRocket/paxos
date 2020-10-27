package echo;

import core.PaxosServer;

import java.io.IOException;

public class Server3 {
    public static void main(String[] args) throws IOException {
        PaxosServer paxosServer=new PaxosServer(2,new EchoCallback());
        paxosServer.start();
    }
}
