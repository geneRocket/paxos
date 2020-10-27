package echo;

import core.StateMachineCallback;

public class EchoCallback implements StateMachineCallback {
    @Override
    public void callback(Object value) {
        System.out.println("statemachine:"+(String)value);
    }
}
