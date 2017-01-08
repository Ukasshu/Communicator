import javax.swing.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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

    HashMap<String, ObjectsSet> objects = new HashMap<>();

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

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

                                //und so weiter
                            }
                            else if (key.isReadable()){
                                SocketChannel socketChannel = (SocketChannel) key.channel();
                            }
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


    private class ReadingThread extends Thread{
        private SocketChannel socketChannel;
        private String id;
        public ReadingThread(SocketChannel socketChannel, String id){
            super(new Runnable() {
                @Override
                public void run() {
                }
            });
            this.socketChannel = socketChannel;
            this.id = id;
        }
    }

    private class MessageHandleThread extends Thread{

    }

}
