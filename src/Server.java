import javax.swing.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by Łukasz on 2017-01-15.
 */
public class Server extends JFrame{
    private JPanel mainPanel;
    private JList usersList;
    private JLabel label;
    private DefaultListModel usersListModel;

    private Thread connectionThread;
    private ArrayList<Thread> handlingThreads = new ArrayList<>();
    private ConcurrentHashMap<String, SocketChannel> sockets = new ConcurrentHashMap<>();
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    private final static String SEP = ""+(char) 198;

    public static void main(String[] args){
        Server server = new Server();
        server.pack();
        server.setVisible(true);
    }

    @Override
    public void dispose() {
        connectionThread.interrupt();
        for(Thread t: handlingThreads){
            t.interrupt();
        }
        System.exit(1);
        super.dispose();
    }

    public Server(){
        setContentPane(mainPanel);
        setResizable(false);
        setTitle("Communicator 2.0 - Server");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        usersListModel = new DefaultListModel();
        usersList.setModel(usersListModel);

        connectionThread = new Thread(new SelectionTask());
        connectionThread.start();
    }

    private class SelectionTask implements Runnable {
        @Override
        public void run() {
            try {
                selector = Selector.open();
                serverSocketChannel = ServerSocketChannel.open();
                InetSocketAddress address = new InetSocketAddress("localhost", 1111);
                serverSocketChannel.bind(address);
                serverSocketChannel.configureBlocking(false);
                SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                Set<SelectionKey> keySet;
                Iterator<SelectionKey> iterator;

                while(true){
                    selector.select();
                    keySet = selector.selectedKeys();
                    iterator = keySet.iterator();

                    while(iterator.hasNext()){
                        SelectionKey key = iterator.next();
                        if(key.isAcceptable()){
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            socketChannel.configureBlocking(false);
                            SelectionKey newKey =socketChannel.register(selector, SelectionKey.OP_READ);
                            newKey.attach(new ConditionObject());
                        }
                        else if(key.isReadable()){
                            try {
                                if(((ConditionObject)key.attachment()).isBool() || ((ConditionObject) key.attachment()).getI() == 1) {
                                    SocketChannel socketChannel = (SocketChannel) key.channel();
                                    ByteBuffer buffer = ByteBuffer.allocate(256);
                                    int bytesRead;
                                    byte[] bytes;
                                    String message;
                                    bytesRead = socketChannel.read(buffer);
                                    if (bytesRead != 0) {
                                        bytes = buffer.array();
                                        message = new String(bytes).trim();
                                        Thread handler = new Thread(new HandlingTask(message, socketChannel));
                                        handlingThreads.add(handler);
                                        handler.start();
                                    }
                                }
                            }catch (IOException e){
                                key.cancel();
                            }
                        }
                        iterator.remove();
                    }
                }

            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private class HandlingTask implements Runnable {
        private String message;
        private SocketChannel socketChannel;
        
        public HandlingTask(String message, SocketChannel socketChannel) {
            this.message = message;
            this.socketChannel = socketChannel;
        }

        @Override
        public void run() {
            try {
                String[] msg = message.split(SEP);
                ByteBuffer buffer;
                byte[] bytes;
                switch (msg[0]) {
                    case "reg":
                        if (sockets.containsKey(msg[1])) {
                            bytes = "ref".getBytes();
                            buffer = ByteBuffer.wrap(bytes);
                            socketChannel.write(buffer);
                            SelectionKey key = socketChannel.keyFor(selector);
                            key.cancel();
                            socketChannel.close();
                        } 
                        else {
                            //socketChannel.keyFor(selector).cancel();
                            SelectionKey key = socketChannel.keyFor(selector);
                            bytes = "acc".getBytes();
                            buffer = ByteBuffer.wrap(bytes);
                            socketChannel.write(buffer);
                            sockets.put(msg[1], socketChannel);
                            usersListModel.addElement(msg[1]);
                            do{
                                buffer = ByteBuffer.allocate(256);
                                socketChannel.read(buffer);
                                bytes = buffer.array();
                            }while(new String(bytes).trim().equals("rdy"+SEP+msg[1]));
                            ((ConditionObject)key.attachment()).setTrue();
                            //socketChannel.register(selector, SelectionKey.OP_READ);
                            sendToAll("jnd"+SEP+msg[1], msg[1]);
                            sendUsers(msg[1], socketChannel);

                        }
                        break;
                    case "msg":
                        bytes = message.getBytes();
                        buffer = ByteBuffer.wrap(bytes);
                        socketChannel.write(buffer);
                        buffer.flip();
                        SocketChannel tmp = sockets.get(msg[2]);
                        tmp.write(buffer);
                        break;
                    case "exi":
                        SelectionKey key = socketChannel.keyFor(selector);
                        if(key!=null)
                            key.cancel();
                        socketChannel.close();
                        sockets.remove(msg[1]);
                        sendToAll("lft"+SEP+msg[1], null);
                        usersListModel.removeElement(msg[1]);
                        break;
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                Thread t = Thread.currentThread();
                handlingThreads.remove(t);
            }
        }
    }

    private void sendUsers(String user, SocketChannel channel) throws IOException, InterruptedException {
        ByteBuffer buffer;
        for(String key : sockets.keySet()){
            if(!key.equals(user)){
                buffer = ByteBuffer.wrap(("jnd"+SEP+key).getBytes());
                channel.write(buffer);
                TimeUnit.MILLISECONDS.sleep(50);
            }
        }
    }

    private void sendToAll(String msg, String except) {
        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
        SocketChannel channel;
        for(String key: sockets.keySet()){
            if(!key.equals(except) ){
                channel = sockets.get(key);
                try {
                    channel.write(buffer);
                    buffer.flip();
                }catch(IOException e){
                    SelectionKey selectionKey = channel.keyFor(selector);
                    selectionKey.cancel();
                    sockets.remove(key);
                }
            }
        }
    }
}
