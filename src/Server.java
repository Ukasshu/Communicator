

import javax.swing.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Łukasz on 2017-01-07.
 */
public class Server extends JFrame{

    private JPanel mainPanel;
    private JLabel status;
    private JList usersList;
    private DefaultListModel usersListModel;
    private Thread connectionThread;

    HashMap<String, SocketChannel> sockets = new HashMap<>();
    ArrayList<Thread> handlingThreads = new ArrayList<>();

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private final String SEP = "" + (char) 198;

    public Server(){
        setContentPane(mainPanel);
        setResizable(false);
        setTitle("Communicator - Serwer");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        usersListModel = new DefaultListModel();
        usersList.setModel(usersListModel);

        connectionThread = new Thread(new Runnable() {
            @Override
            public void run ()  {
                try {
                    selector = Selector.open();
                    serverSocketChannel = ServerSocketChannel.open();
                    InetSocketAddress address = new InetSocketAddress("localhost", 1111);
                    serverSocketChannel.bind(address);
                    serverSocketChannel.configureBlocking(false);
                    SelectionKey selectionKey = serverSocketChannel.register(selector, serverSocketChannel.validOps(), null);
                    while (true){
                        selector.select();
                        Set<SelectionKey> keySet = selector.selectedKeys();
                        Iterator<SelectionKey> iterator = keySet.iterator();

                        while (iterator.hasNext()){
                            SelectionKey key = iterator.next();
                            if (key.isAcceptable()){
                                SocketChannel socketChannel = serverSocketChannel.accept();
                                socketChannel.configureBlocking(false);
                                socketChannel.register(selector, SelectionKey.OP_READ );
                            }
                            else if (key.isReadable()){
                                SocketChannel socketChannel = (SocketChannel) key.channel();
                                ByteBuffer buffer = ByteBuffer.allocate(256);
                                int bytesRead;
                                byte[] bytes = new byte[0];
                                try {
                                    bytesRead = socketChannel.read(buffer);
                                    bytes = buffer.array();
                                    Thread handler = new Thread(new MessageHandling(new String(bytes), socketChannel));
                                    handlingThreads.add(handler);
                                    handler.start();
                                }catch (IOException e){
                                    e.printStackTrace();
                                }
                            }
                            iterator.remove();
                        }
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
        connectionThread.start();
    }

    public static void main(String [] args){
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Server server = new Server();
        server.pack();
        server.setVisible(true);
    }


    @Override
    public void dispose() {

        super.dispose();
    }

    private static void sendMessage(String msg, SocketChannel sc){

    }

    private class MessageHandling implements Runnable{
        private String message;
        private SocketChannel socketChannel;

        public MessageHandling(String message, SocketChannel socketChannel){
            this.message = message;
            this.socketChannel = socketChannel;
        }

        @Override
        public void run() {
            try {
                String[] msg = message.split(SEP);
                ByteBuffer buffer = ByteBuffer.allocate(256);
                byte[] bytes;
                switch (msg[0]) {
                    case "reg":
                        if (sockets.containsKey(msg[1])) {
                            bytes = (("ref" + SEP + SEP + SEP)).getBytes();
                            buffer.wrap(bytes);
                            socketChannel.write(buffer);
                            SelectionKey key = socketChannel.keyFor(selector);
                            key.cancel();
                            socketChannel.close();
                        } else {
                            bytes = (("acc" + SEP + SEP + SEP)).getBytes();
                            buffer.wrap(bytes);
                            socketChannel.write(buffer);
                            sockets.put(msg[1], socketChannel);
                            usersListModel.addElement(msg[1]);
                            sendToAll("jnd"+SEP+msg[1]);
                        }
                        break;
                    case "msg":
                        if (sockets.containsKey(msg[2])) {
                            SocketChannel receiver = sockets.get(msg[1]);
                            bytes = message.getBytes();
                            buffer.wrap(bytes);
                            receiver.write(buffer);
                            socketChannel.write(buffer);
                        } else{
                            bytes = ("msg"+SEP+msg[1]+SEP+msg[2]+SEP+"UŻYTKOWNIK NIE JEST DOSTĘPNY").getBytes();
                            buffer.wrap(bytes);
                            socketChannel.write(buffer);
                        }
                            break;
                    case "exi":
                        SelectionKey key = socketChannel.keyFor(selector);
                        key.cancel();
                        socketChannel.close();
                        sockets.remove(msg[1]);
                        sendToAll("lft"+SEP+msg[1]);
                        usersListModel.removeElement(msg[1]);
                        break;

                }
            }catch (IOException e){
                e.printStackTrace();
            }
            finally {
                Thread t =Thread.currentThread();
                handlingThreads.remove(t);
            }
        }

        private void sendToAll(String msg) throws IOException{
            ByteBuffer b = ByteBuffer.allocate(256);
            b.wrap(msg.getBytes());
            for(SocketChannel s :sockets.values()){
                s.write(b);
            }
        }
    }

}
