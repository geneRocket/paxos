package core;

import java.util.Comparator;
import java.util.PriorityQueue;

public class DelayExecRecord {
    int default_delay;

    public DelayExecRecord(int default_delay){
        this.default_delay=default_delay;
    }

    enum EventType {
        PrepareCheck, AcceptCheck
    }

    static class TimeObject {
        long absolute_time;
        EventType eventType;
        int instance_id;
    }


    PriorityQueue<TimeObject> time_point_set = new PriorityQueue<>(new Comparator<TimeObject>() {
        @Override
        public int compare(TimeObject o1, TimeObject o2) {
            return Long.compare(o1.absolute_time, o2.absolute_time);
        }
    });


    void add_time_record(int delay_time, EventType eventType, int instance_id) {
        TimeObject timeObject = new TimeObject();
        timeObject.absolute_time = System.currentTimeMillis() + delay_time;
        timeObject.eventType = eventType;
        timeObject.instance_id = instance_id;
        time_point_set.add(timeObject);
        System.out.println("time_point_set size");
        System.out.println(time_point_set.size());
    }

    long get_next_time_left() {
        TimeObject timeObject = time_point_set.peek();
        if(timeObject==null){
            return default_delay;
        }
        return timeObject.absolute_time - System.currentTimeMillis();
    }

    TimeObject take_soonest_delay_exec() {
        return time_point_set.poll();
    }

}
