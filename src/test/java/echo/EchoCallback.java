package echo;

import core.StateMachineCallback;

public class EchoCallback implements StateMachineCallback {
    int num=0;
    @Override
    public void callback(Object value) {
        System.out.println("statemachine:"+(String)value);
        if(Integer.valueOf(num).toString().equals((String)value)){
            num++;
        }
        else {
            System.out.println("num error");
        }
    }
}
