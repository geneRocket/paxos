package Network;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

public interface Send {

    void send_to(String ip,int port,byte[] msg) throws IOException;

    default void send_to(String ip, int port, Object object) throws IOException {
//        String json = JSON.toJSONString(object);
//        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
        byte[] bytes=byteArrayOutputStream.toByteArray();
        objectOutputStream.close();
        byteArrayOutputStream.close();

        send_to(ip,port,bytes);
    }

}
