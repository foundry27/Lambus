import me.foundry.lambus.Lambus;
import me.foundry.lambus.Link;
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
        Link<BasicEvent> link = bus.subscribeDirect((BasicEvent e) -> {});
        final long last = bean.getCurrentThreadCpuTime();

        for (int i = 0; i < 100000000; i++) {
            bus.post(new BasicEvent());
        }

        final long current = bean.getCurrentThreadCpuTime() - last;
        bus.unsubscribeDirect(link);

        System.out.println(current);
    }

    @Prioritized(Priority.LOWEST)
    private Link<BasicEvent> onBasicEventLow = (e) -> {
        System.out.println("Low Priority!");
    };

    private Link<BasicEvent> onBasicEventNormal = (e) -> {
        System.out.println("Normal Priority");
    };

    @Prioritized(Priority.HIGHEST)
    private Link<BasicEvent> onBasicEventHigh = (e) -> {
        System.out.println("High Priority");
    };

    public void testPriorityHandling() {
        bus.subscribeAll(this);
        bus.post(new BasicEvent());
        bus.unsubscribeAll(this);
    }

    public void testSubscriptionTime() {
        long last = bean.getCurrentThreadCpuTime();
        for (int i = 0; i < 100000; i++) {
            bus.subscribeAll(this);
        }
        final long current = bean.getCurrentThreadCpuTime() - last;
        bus.unsubscribeAll(this);
        System.out.println(current);

    }

    public void testUnsubscription() {
        bus.subscribeAll(this);
        bus.post(new BasicEvent());
        bus.unsubscribeAll(this);
        bus.post(new BasicEvent());
    }

    public void testConstruction() {
        bus.subscribeDirect((BasicEvent e) -> {});
    }

}
