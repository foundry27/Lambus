package me.foundry.lambus.internal;

import me.foundry.lambus.Lambus;
import me.foundry.lambus.Subscribed;
import me.foundry.lambus.Subscriber;
import me.foundry.lambus.event.Event;
import me.foundry.lambus.filter.Filter;
import me.foundry.lambus.filter.Filtered;
import me.foundry.lambus.internal.util.LambdaUtils;
import me.foundry.lambus.internal.util.SubscriberList;
import me.foundry.lambus.priority.Prioritized;
import me.foundry.lambus.priority.Priority;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Mark Johnson
 */

public final class LambusImpl implements Lambus {
    private final ConcurrentMap<Class<? extends Event>, SubscriberList> classsubMap = new ConcurrentHashMap<>();

    @Override
    public boolean subscribe(Class<? extends Event> e, Object o) {
        Objects.requireNonNull(o);
        boolean added = false;
        for (final Field field : o.getClass().getDeclaredFields()) {
            if (field.getType().equals(Subscriber.class) && field.isAnnotationPresent(Subscribed.class)) {
                try {
                    field.setAccessible(true);
                    final Subscriber<?> sub = (Subscriber<?>) field.get(o);
                    final Class<? extends Event> reifiedClass = LambdaUtils.getLambdaTarget(sub);
                    if (reifiedClass.equals(e)) {
                        this.classsubMap.computeIfAbsent(reifiedClass, l -> new SubscriberList()).add(new SubscriptionData<>(
                                sub,
                                reifiedClass,
                                findPriority(field),
                                findFilters(field))
                        );
                        added = true;
                    }
                } catch (IllegalAccessException | SecurityException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return added;
    }

    @Override
    public boolean unsubscribe(Class<? extends Event> e, Object o) {
        Objects.requireNonNull(o);
        boolean removed = false;
        for (final Field field : o.getClass().getDeclaredFields()) {
            if (field.getType().equals(Subscriber.class) && field.isAnnotationPresent(Subscribed.class)) {
                try {
                    field.setAccessible(true);
                    final Subscriber<?> sub = (Subscriber<?>) field.get(o);
                    final Class<? extends Event> reifiedClass = LambdaUtils.getLambdaTarget(sub);
                    if (reifiedClass.equals(e)) {
                        final SubscriberList subs = classsubMap.get(reifiedClass);
                        if (sub != null) {
                            removed |= subs.removeIf(subData -> subData.getSubscriber() == sub);
                        }
                    }
                } catch (IllegalAccessException | SecurityException ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        }
        return removed;
    }

    @Override
    public <T extends Event> Subscriber<T> subscribeDirect(Subscriber<T> sub) {
        final Class<? extends Event> reifiedClass = LambdaUtils.getLambdaTarget(sub);
        this.classsubMap.computeIfAbsent(reifiedClass, l -> new SubscriberList()).add(new SubscriptionData<>(
                sub,
                reifiedClass,
                Priority.NORMAL,
                null)
        );
        return sub;
    }

    @Override
    public boolean unsubscribeDirect(Subscriber<?> sub) {
        boolean removed = false;
        final SubscriberList subs = classsubMap.get(LambdaUtils.getLambdaTarget(sub));
        if (subs != null) {
            removed = subs.removeIf(subData -> subData.getSubscriber() == sub);
        }
        return removed;
    }

    @Override
    public boolean subscribe(Object o) {
        Objects.requireNonNull(o);
        boolean added = false;
        for (final Field field : o.getClass().getDeclaredFields()) {
            if (field.getType().equals(Subscriber.class) && field.isAnnotationPresent(Subscribed.class)) {
                try {
                    field.setAccessible(true);
                    final Subscriber<?> sub = (Subscriber<?>) field.get(o);
                    final Class<? extends Event> reifiedClass = LambdaUtils.getLambdaTarget(sub);
                    this.classsubMap.computeIfAbsent(reifiedClass, l -> new SubscriberList()).add(new SubscriptionData<>(
                            sub,
                            reifiedClass,
                            findPriority(field),
                            findFilters(field))
                    );
                    added = true;
                } catch (IllegalAccessException | SecurityException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return added;
    }

    @Override
    public boolean unsubscribe(Object o) {
        Objects.requireNonNull(o);
        boolean removed = false;
        for (final Field field : o.getClass().getDeclaredFields()) {
            if (field.getType().equals(Subscriber.class) && field.isAnnotationPresent(Subscribed.class)) {
                try {
                    field.setAccessible(true);
                    removed |= unsubscribeDirect((Subscriber<?>) field.get(o));
                } catch (IllegalAccessException | SecurityException ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        }
        return removed;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event> T post(T event) {
        Objects.requireNonNull(event);
        final SubscriberList list = classsubMap.get(event.getClass());
        if (list != null) {
            OUTER: for (SubscriptionData sub : list) {
                final Subscriber<T> castSub = (Subscriber<T>) sub.getSubscriber();
                if (sub.getFilters() != null) {
                    for (Filter<T> f : sub.getFilters()) {
                        if (!f.test(castSub, event)) continue OUTER;
                    }
                }
                castSub.invoke(event);
            }
        }
        return event;
    }

    private static Priority findPriority(Field field) {
        final Prioritized priorityAnnotation = field.getAnnotation(Prioritized.class);
        return priorityAnnotation != null ? priorityAnnotation.value() : Priority.NORMAL;
    }

    private static Filter[] findFilters(Field field) {
        final Filtered filterAnnotation = field.getAnnotation(Filtered.class);
        Filter[] filters = null;
        if (filterAnnotation != null) {
            try {
                filters = new Filter[filterAnnotation.value().length];
                for (int i = 0; i < filterAnnotation.value().length; i++) {
                    filters[i] = filterAnnotation.value()[i].newInstance();
                }
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return filters;
    }

}
