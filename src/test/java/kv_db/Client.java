package kv_db;

import core.PaxosClinet;

import java.io.IOException;

public class Client {

    public static void main(String[] args) throws IOException {
        PaxosClinet paxosClinet=new PaxosClinet();
        for(int i=0;i<1000;i++){
            paxosClinet.submit_value(Integer.valueOf(i).toString());
        }
        paxosClinet.flush();
    }
}
