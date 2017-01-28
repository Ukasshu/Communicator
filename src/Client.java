import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Łukasz on 2017-01-15.
 */
public class Client {

    private final static String SEP = ""+(char) 198;

    private SocketChannel socketChannel;
    private Selector selector;
    private SelectionKey selectionKey;
    private JFrame frame;
    private DefaultListModel listModel;
    private String myUsername;
    private HashMap<String, ChatFrame> chatFrames = new HashMap<>();

    public static void main(String[] args){
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Client client = new Client();
        client.start();
    }

    private void start() {
        frame = new LoginFrame();
        frame.pack();
        frame.setVisible(true);
    }

    /////////////////////////////////////////////////////////////
    public class LoginFrame extends JFrame{
        private JTextField loginTextField;
        private JButton connectButton;
        private JLabel label;
        private JPanel mainPanel;

        public LoginFrame(){
            setContentPane(mainPanel);
            setResizable(false);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            loginTextField.setDocument(new LimitedCharactersDocument(15));
            connectButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try{
                        ByteBuffer buffer;
                        byte[] bytes;
                        socketChannel = SocketChannel.open();
                        socketChannel.configureBlocking(false);
                        socketChannel.connect(new InetSocketAddress("localhost", 1111));
                        while(!socketChannel.finishConnect()){};
                        selector  = Selector.open();
                        selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
                        buffer = ByteBuffer.wrap(("reg"+SEP+loginTextField.getText()).getBytes());
                        socketChannel.write(buffer);
                        int bytesRead;
                        selector.select();
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iter = selectionKeys.iterator();
                        while(iter.hasNext()){
                            SelectionKey key = iter.next();
                            if(key.isReadable()) {
                                SocketChannel s = (SocketChannel) key.channel();
                                buffer = ByteBuffer.allocate(256);
                                bytesRead = s.read(buffer);
                            }
                            iter.remove();
                        }
                        bytes = buffer.array();
                        String message = new String(bytes).trim();
                        if(!message.equals("acc")){
                            selectionKey.cancel();
                            socketChannel.close();
                            JOptionPane.showMessageDialog(mainPanel, "Spróbuj użyć innej nazwy");
                        }
                        else {
                            myUsername = loginTextField.getText();
                            frame = new MainFrame();
                            frame.pack();
                            frame.setVisible(true);
                            LoginFrame.this.dispose();
                        }

                    }catch (IOException exc){
                        exc.printStackTrace();

                    }
                }
            });
        }

    }
    /////////////////////////////////////////////////////////////
    public class MainFrame extends JFrame{

        private JList usersList;
        private JButton writeButton;
        private JPanel mainPanel;
        private Thread readingThread;
        private ArrayList<Thread> handlingThreads = new ArrayList<>();


        public MainFrame(){
            setContentPane(mainPanel);
            setTitle("Communicator 2.0 - "+ myUsername  +" - Client");
            setResizable(false);
            listModel = new DefaultListModel();
            usersList.setModel(listModel);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            writeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(!usersList.isSelectionEmpty()){
                        String user = (String)usersList.getSelectedValue();
                        ChatFrame c = new ChatFrame(user);
                        chatFrames.put(user, c);
                        c.pack();
                        c.setVisible(true);
                    }
                }
            });
            readingThread = new Thread(new ReadingTask());
            readingThread.start();
        }

        private class ReadingTask implements Runnable{
            @Override
            public void run() {
                try {
                    String message = "rdy" + SEP + myUsername;
                    byte[] bytes = message.getBytes();
                    ByteBuffer buffer = ByteBuffer.wrap(bytes);
                    socketChannel.write(buffer);
                    while(true){
                        try {
                            selector.select();
                            Set<SelectionKey> keySet = selector.selectedKeys();
                            Iterator<SelectionKey> iterator = keySet.iterator();
                            while(iterator.hasNext()){
                                SelectionKey key = iterator.next();
                                if(key.isReadable()){
                                    SocketChannel s = (SocketChannel)key.channel();
                                    buffer = ByteBuffer.allocate(256);
                                    s.read(buffer);
                                    bytes = buffer.array();
                                    message = new String(bytes).trim();
                                    Thread handler = new Thread(new HandlingTask(message));
                                    handlingThreads.add(handler);
                                    handler.start();
                                }
                                iterator.remove();
                            }
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                }catch (Exception e){
                    selectionKey.cancel();
                    selectionKey.cancel();
                }
            }
        }

        private class HandlingTask implements Runnable{
            private String message;
            public HandlingTask(String message){
                this.message = message;
            }
            @Override
            public void run() {
                try{
                    String[] msg = message.split(SEP);
                    switch (msg[0]){
                        case "jnd":
                           listModel.addElement(msg[1]);
                           break;
                        case "lft":
                            listModel.removeElement(msg[1]);
                            break;
                        case "msg":
                            String dst;
                            if(msg[1].equals(myUsername)){
                                dst = msg[2];
                            }
                            else{
                                dst =msg[1];
                            }
                            if(!chatFrames.containsKey(dst)){
                                ChatFrame c = new ChatFrame(dst);
                                chatFrames.put(dst, c);
                                c.pack();
                                c.setVisible(true);
                            }
                            JTextArea textArea = chatFrames.get(dst).getChatTextArea();
                            textArea.insert(msg[1]+"\n", textArea.getText().length());
                            textArea.insert(msg[3]+"\n\n", textArea.getText().length());
                            break;
                    }
                }finally {
                    Thread t = Thread.currentThread();
                    handlingThreads.remove(t);
                }
            }
        }

        @Override
        public void dispose(){
            try{
                ByteBuffer buffer = ByteBuffer.wrap(("exi"+SEP+myUsername).getBytes());
                socketChannel.write(buffer);
            }catch (IOException e){
                e.printStackTrace();
            }
            System.exit(0);
            super.dispose();
        }


    }
    /////////////////////////////////////////////////////////////
    public class ChatFrame extends JFrame{

        private JTextArea chatTextArea;
        private JTextArea messageTextArea;
        private JButton sendButton;
        private JPanel mainPanel;

        private String user;

        public  ChatFrame(String user){
            setContentPane(mainPanel);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setTitle("Piszesz do "+ user + " - "+myUsername);
            //setResizable(false);
            this.user = user;
            chatTextArea.setDocument(new LimitedCharactersDocument(220));
            sendButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String message = messageTextArea.getText();
                    if(message.length()!=0){
                        message = "msg"+SEP+myUsername+SEP+user+SEP+message;
                        byte[] bytes = message.getBytes();
                        ByteBuffer buffer = ByteBuffer.wrap(bytes);
                        try {
                            socketChannel.write(buffer);
                        }catch (IOException exc){
                            exc.printStackTrace();
                        }
                    }
                    messageTextArea.setText("");
                }
            });
        }

        public JTextArea getChatTextArea(){
            return chatTextArea;
        }

        @Override
        public void dispose() {
            chatFrames.remove(user);
            super.dispose();
        }
    }
}
