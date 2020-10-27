package Network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class NioSend implements Send {

    private int int_size=4;
    private Selector selector;
    private ConcurrentHashMap<SocketChannel, ByteBuffer> to_send_buffer = new ConcurrentHashMap<>();

    public NioSend() throws IOException {
        selector=Selector.open();

        new Thread(() -> {
            while (true){
                try {
                    selector.select();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                selector.selectedKeys();
                Iterator<SelectionKey> iter=selector.selectedKeys().iterator();
                while (iter.hasNext()){

                    SelectionKey key=iter.next();
                    iter.remove();
                    SocketChannel socketChannel=(SocketChannel)key.channel();
                    if(key.isConnectable()){
                        try {
                            if(socketChannel.finishConnect()){
                                socketChannel.register(selector,SelectionKey.OP_WRITE);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else if(key.isWritable()){
                        //key.interestOps(key.interestOps() & (~ SelectionKey.OP_WRITE));
                        try {
                            writeData(socketChannel);
                            key.cancel();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();

    }

    void writeData(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = to_send_buffer.get(socketChannel);
        socketChannel.write(byteBuffer);
        assert byteBuffer.remaining()==0;
        to_send_buffer.remove(socketChannel);
        socketChannel.close();
    }



    @Override
    public void send_to(String ip, int port, byte[] msg) throws IOException {

        SocketChannel socketChannel=SocketChannel.open();

        ByteBuffer byteBuffer=ByteBuffer.wrap(msg);
        to_send_buffer.put(socketChannel,byteBuffer);

        socketChannel.configureBlocking(false);
        SocketAddress socketAddress=new InetSocketAddress(ip,port);
        socketChannel.connect(socketAddress);

        socketChannel.register(selector, SelectionKey.OP_CONNECT);

        selector.wakeup();
    }


}
