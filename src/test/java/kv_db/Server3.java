package kv_db;

import core.PaxosServer;

import java.io.IOException;

public class Server3 {
    public static void main(String[] args) throws IOException {
        PaxosServer paxosServer=new PaxosServer(2,new KVcallback());
        paxosServer.start();
    }
}
