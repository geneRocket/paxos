package Network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
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
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NioRecv implements Recv {
    String ip;
    int port;
    Selector selector ;
    ServerSocketChannel serverSocketChannel;
    private Map<SocketChannel, ByteArrayOutputStream> buffer_map = new HashMap<>();
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    BlockingQueue<byte[]> msg_queue= new LinkedBlockingQueue<byte[]>();

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
                    Set<SelectionKey> selectionKeySet=selector.selectedKeys();
                    Iterator<SelectionKey> iter=selectionKeySet.iterator();

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
                            read_data(socketChannel);
                        }
                    }

                }
            }
        }).start();
    }

    void read_data(SocketChannel socketChannel){
        try {
            int len=socketChannel.read(byteBuffer);
            if(len==-1){
                ByteArrayOutputStream byteArrayOutputStream=buffer_map.get(socketChannel);
                if(byteArrayOutputStream.size()>0){
                    msg_queue.add(byteArrayOutputStream.toByteArray());
                }
                buffer_map.remove(socketChannel);
                socketChannel.close();
            }
            if(!buffer_map.containsKey(socketChannel)){
                buffer_map.put(socketChannel,new ByteArrayOutputStream());
            }
            byteBuffer.flip();
            buffer_map.get(socketChannel).write(byteBuffer.array());
            byteBuffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }


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
