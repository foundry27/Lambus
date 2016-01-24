import me.foundry.lambus.Event;
import me.foundry.lambus.Lambus;
import me.foundry.lambus.Link;
import me.foundry.lambus.internal.SynchronousLambus;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Created by Mark on 1/24/2016.
 */
public class EventTester {
    private final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    private final Lambus bus = new SynchronousLambus();

    private Link<BasicEvent> onBasicEvent = (e) -> {};

    public void testDispatchTime() {
        bus.subscribeAll(this);
        final long last = bean.getCurrentThreadCpuTime();

        for (int i = 0; i < 100000000; i++) {
            bus.post(new BasicEvent());
        }

        final long current = bean.getCurrentThreadCpuTime() - last;
        bus.unsubscribeAll(this);

        System.out.println(current);
    }

    public void testInlineDispatch() {
        Link<?> l = bus.subscribeDirect((BasicEvent e) -> System.out.println(e.getClass().getName()));
        bus.post(new BasicEvent());

        bus.unsubscribeDirect(l);
        bus.post(new BasicEvent());
    }

}
