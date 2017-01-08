import java.io.Serializable;

/**
 * Created by Łukasz on 2017-01-07.
 */
public class Message implements Serializable{
    private String receiverId;
    private String senderId;
    private String type;
    private String content;

    public Message (String receiverId, String senderId, String type, String content){
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.type = type;
        this.content = content;
    }

    public String getReceiverId(){
        return receiverId;
    }

    public String getSenderId(){
        return senderId;
    }

    public String getType(){
        return type;
    }

    public String getConten(){
        return content;
    }
}
