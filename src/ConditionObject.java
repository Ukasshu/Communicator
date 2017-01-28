/**
 * Created by Łukasz on 2017-01-15.
 */
public class ConditionObject {
    private boolean bool;
    private int i;

    public ConditionObject(){
        bool = false;
        i = 0;
    }



    public void setTrue(){
        bool = true;
    }

    public boolean isBool() {
        return bool;
    }

    public int getI(){
        return ++i;
    }
}
