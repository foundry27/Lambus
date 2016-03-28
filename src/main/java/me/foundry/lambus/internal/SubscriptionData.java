package me.foundry.lambus.internal;

import me.foundry.lambus.Subscriber;
import me.foundry.lambus.event.Event;
import me.foundry.lambus.filter.Filter;
import me.foundry.lambus.priority.Priority;

/**
 * @author Mark Johnson
 */
public class SubscriptionData<T extends Event> implements Comparable<SubscriptionData> {
    private final Subscriber<T> subscriber;
    private final Class<? extends Event> clazz;
    private final Priority priority;
    private final Filter[] filters;

    public SubscriptionData(Subscriber<T> subscriber,
                            Class<? extends Event> clazz,
                            Priority priority,
                            Filter[] filters) {
        this.subscriber = subscriber;
        this.clazz = clazz;
        this.priority = priority;
        this.filters = filters;
    }

    public Subscriber<T> getSubscriber() {
        return this.subscriber;
    }

    public Class<? extends Event> getEventClass() {
        return this.clazz;
    }

    public Priority getPriority() {
        return this.priority;
    }

    public Filter[] getFilters() {
        return this.filters;
    }

    @Override
    public int compareTo(SubscriptionData o) {
        return this.priority.ordinal() - o.getPriority().ordinal();
    }
}
