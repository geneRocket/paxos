package Network;

import lombok.ToString;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@ToString
class SendWindow{
    String ip;
    int port;
    ByteBuffer byteBuffer;
}


public class NioSend implements Send {

    private final static int int_size = 4;
    private ConcurrentHashMap<Long,SocketChannel> id_socketChannel_map = new ConcurrentHashMap<>();
    private BlockingQueue<SendWindow> send_queue=new LinkedBlockingQueue<>();

    public NioSend() throws IOException {

        new Thread(() -> {
            while (true) {
                SendWindow sendWindow = null;
                try {
                    sendWindow= send_queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(sendWindow==null)
                    continue;

                Long id;
                try {
                    id=get_address_id(sendWindow.ip,sendWindow.port);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                if(!id_socketChannel_map.containsKey(id)){
                    SocketChannel socketChannel= null;
                    try {
                        socketChannel = new_socket_channel(sendWindow.ip,sendWindow.port);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    assert socketChannel != null;
                    id_socketChannel_map.put(id,socketChannel);

                }

                SocketChannel socketChannel=id_socketChannel_map.get(id);
                try {
                    socketChannel.write(sendWindow.byteBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                assert sendWindow.byteBuffer.remaining()==0;


            }

        }).start();

    }

    SocketChannel new_socket_channel(String ip,int port) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        SocketAddress socketAddress = new InetSocketAddress(ip, port);
        socketChannel.connect(socketAddress);

        socketChannel.configureBlocking(false);

        return socketChannel;
    }



    @Override
    public void send_to(String ip, int port, byte[] msg) {


        ByteBuffer byteBuffer = ByteBuffer.allocate(int_size + msg.length);
        byteBuffer.putInt(msg.length);
        byteBuffer.put(msg);
        byteBuffer.flip();

        SendWindow sendWindow=new SendWindow();
        sendWindow.ip=ip;
        sendWindow.port=port;
        sendWindow.byteBuffer=byteBuffer;
        try {
            send_queue.put(sendWindow);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int ipToInt(String ip) throws Exception {
        if(ip.equals("localhost"))
            ip="127.0.0.1";
        String[] ipAry = ip.split("\\.");
        if (ipAry.length != 4) {
            throw new Exception("ipToInt error ip:" + ip);
        }
        int[] ipBuf = new int[4];
        for (int i = 0; i < 4; i++) {
            int item = Integer.parseInt(ipAry[i]);
            ipBuf[i] = item;
        }

        int s;
        int s0 = ipBuf[0] & 0xff;// 最低位
        int s1 = ipBuf[1] & 0xff;
        int s2 = ipBuf[2] & 0xff;
        int s3 = ipBuf[3] & 0xff;
        s3 <<= 24;
        s2 <<= 16;
        s1 <<= 8;
        s = s0 | s1 | s2 | s3;
        return s;
    }

    private Long get_address_id(String sip, int iport) throws Exception {
        int ipInt = ipToInt(sip);
        return ((long) ipInt) << 32 | iport;
    }
}
