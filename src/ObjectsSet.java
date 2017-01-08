import java.nio.channels.SocketChannel;

/**
 * Created by Łukasz on 2017-01-08.
 */
public class ObjectsSet {
    private String id;
    private SocketChannel socketChannel;
    private Thread readingThread;

    public ObjectsSet(String id, SocketChannel socketChannel, Thread readingThread){
        this.id = id;
        this.socketChannel = socketChannel;
        this.readingThread = readingThread;
    }

    public String getId(){
        return id;
    }

    public SocketChannel getSocketChannel(){
        return socketChannel;
    }

    public Thread getReadingThread(){
        return readingThread;
    }
}
