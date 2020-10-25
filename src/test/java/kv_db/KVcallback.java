package kv_db;

import core.StateMachineCallback;

public class KVcallback implements StateMachineCallback {
    @Override
    public void callback(Object value) {
        System.out.println("statemachine:"+(String)value);
    }
}
