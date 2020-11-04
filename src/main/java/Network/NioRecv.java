package Network;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NioRecv implements Recv {
    String ip;
    int port;

    Selector selector ;
    ServerSocketChannel serverSocketChannel;

    final static int int_size=4;
    static class Buffer{
        ByteBuffer byteBuffer;
        boolean is_reading_len;

        public Buffer(){
            reset();
        }

        void reset(){
            byteBuffer=ByteBuffer.allocate(int_size);
            is_reading_len=true;
        }
    }

    private Map<SocketChannel, Buffer> buffer_map = new HashMap<>();

    BlockingQueue<byte[]> msg_queue= new LinkedBlockingQueue<>();

    public NioRecv(String ip,int port) throws IOException {
        this.ip=ip;
        this.port=port;

        selector=Selector.open();
        serverSocketChannel=ServerSocketChannel.open();
        SocketAddress socketAddress = new InetSocketAddress(port);
        serverSocketChannel.bind(socketAddress);
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        selector.select();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Iterator<SelectionKey> iter=selector.selectedKeys().iterator();;

                    while(iter.hasNext()){
                        SelectionKey selectionKey =iter.next();
                        iter.remove();
                        if(selectionKey.isAcceptable()){
                            try {
                                handle_accept();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else if(selectionKey.isReadable()){
                            SocketChannel socketChannel=(SocketChannel)selectionKey.channel();
                            if(!read_data(socketChannel)){
                                try {
                                    socketChannel.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                selectionKey.cancel();
                            }
                        }
                    }

                }
            }
        }).start();
    }

    boolean read_data(SocketChannel socketChannel) {
        if(!buffer_map.containsKey(socketChannel)){
            buffer_map.put(socketChannel,new Buffer());
        }
        Buffer buffer=buffer_map.get(socketChannel);
        assert buffer.byteBuffer.remaining()>0;

        try {
            int len=socketChannel.read(buffer.byteBuffer);
            if(len==-1){
                buffer_map.remove(socketChannel);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(buffer.byteBuffer.remaining()==0){
            buffer.byteBuffer.flip();

            if(buffer.is_reading_len){
                buffer.is_reading_len=false;
                int packet_len=buffer.byteBuffer.getInt();
                buffer.byteBuffer= ByteBuffer.allocate(packet_len);
            }
            else {
                try {
                    msg_queue.put(buffer.byteBuffer.array());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                buffer.reset();
            }
        }
        return true;
    }

    void handle_accept() throws IOException {
        SocketChannel socketChannel=serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector,SelectionKey.OP_READ);
    }

    @Override
    public byte[] receive() throws InterruptedException {
        return msg_queue.take();
    }


}
