package Network;

import Network.NetworkPacket.Packet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;

public interface Recv {

    byte[] receive() throws InterruptedException;

    default Object receive_object() throws InterruptedException, IOException {
        byte[] bytes = receive();
//        String json=new String(bytes,StandardCharsets.UTF_8);
//        return JSON.parseObject(json, Packet.class);

        ByteArrayInputStream byteArrayInputStream= new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        try {
            return objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
