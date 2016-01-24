import me.foundry.lambus.Lambus;
import me.foundry.lambus.Link;
import me.foundry.lambus.internal.SynchronousLambus;

/**
 * Created by Mark on 1/24/2016.
 */
public class Main {
    public static void main(String[] args) {
        final EventTester et = new EventTester();
        et.testDispatchTime();
        et.testInlineDispatch();
    }
}
