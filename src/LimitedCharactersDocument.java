import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Created by Łukasz on 2017-01-14.
 */
public class LimitedCharactersDocument extends PlainDocument{
    private int limit;
    public LimitedCharactersDocument(int limit){
        super();
        this.limit = limit;

    }

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        if(str == null) return;
        if((getLength() + str.length())<=limit) {
            super.insertString(offs, str, a);
        }
    }
}
