import me.foundry.lambus.Lambus;
import me.foundry.lambus.Subscribed;
import me.foundry.lambus.Subscriber;
import me.foundry.lambus.internal.LambusImpl;
import me.foundry.lambus.priority.Prioritized;
import me.foundry.lambus.priority.Priority;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Created by Mark on 1/24/2016.
 */
public class EventTester {
    private final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    private final Lambus bus = new LambusImpl();

    public void testDispatchTime() {
        Subscriber<BasicEvent> link = bus.subscribeDirect((BasicEvent e) -> {});
        final long last = bean.getCurrentThreadCpuTime();

        for (int i = 0; i < 100000000; i++) {
            bus.post(new BasicEvent());
        }

        final long current = bean.getCurrentThreadCpuTime() - last;
        bus.unsubscribeDirect(link);

        System.out.println(current);
    }

    @Subscribed
    @Prioritized(Priority.LOWEST)
    private Subscriber<BasicEvent> onBasicEventLowest    = (e) -> System.out.println("Lowest Priority");

    @Subscribed
    @Prioritized(Priority.LOW)
    private Subscriber<BasicEvent> onBasicEventLow       = (e) -> System.out.println("Low Priority");

    @Subscribed
    @Prioritized(Priority.NORMAL)
    private Subscriber<BasicEvent> onBasicEventNormal    = (e) -> System.out.println("Normal Priority");

    @Subscribed
    @Prioritized(Priority.HIGH)
    private Subscriber<BasicEvent> onBasicEventHigh      = (e) -> System.out.println("High Priority");

    @Subscribed
    @Prioritized(Priority.HIGHEST)
    private Subscriber<BasicEvent> onBasicEventHighest   = (e) -> System.out.println("Highest Priority");

    public void testPriorityHandling() {
        for (int i = 0; i < 5; i++) {
            bus.subscribe(this);
        }
        bus.post(new BasicEvent());
        bus.unsubscribe(this);
    }

    public void testSubscriptionTime() {
        long last = bean.getCurrentThreadCpuTime();
        for (int i = 0; i < 100000; i++) {
            bus.subscribe(this);
        }
        final long current = bean.getCurrentThreadCpuTime() - last;
        bus.unsubscribe(this);
        System.out.println(current);

    }

    public void testUnsubscription() {
        bus.subscribe(this);
        bus.post(new BasicEvent());
        bus.unsubscribe(this);
        bus.post(new BasicEvent());
    }

    public void testConstruction() {
        bus.subscribeDirect((BasicEvent e) -> System.out.println("dank memes"));
        bus.post(new BasicEvent());
    }

}
