import me.foundry.lambus.Subscriber;
import me.foundry.lambus.event.Event;
import me.foundry.lambus.filter.Filter;

/**
 * Created by Mark on 1/24/2016.
 */
public class AlwaysFalseFilter<T extends Event> implements Filter<T> {
    @Override
    public boolean test(Subscriber<T> link, T event) {
        return false;
    }
}
