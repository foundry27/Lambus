package me.foundry.lambus.internal;

import me.foundry.lambus.Lambus;
import me.foundry.lambus.Link;
import me.foundry.lambus.event.Event;
import me.foundry.lambus.filter.Filter;
import me.foundry.lambus.filter.Filtered;
import me.foundry.lambus.internal.util.LambdaUtils;
import me.foundry.lambus.priority.Prioritized;
import me.foundry.lambus.priority.Priority;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author Mark Johnson
 */

public final class LambusImpl implements Lambus {
    private final ConcurrentMap<Class<? extends Event>, BlockingQueue<LinkData>> classLinkMap = new ConcurrentHashMap<>();

    @Override
    public boolean subscribe(Class<? extends Event> e, Object o) {
        Objects.requireNonNull(o);
        boolean added = false;
        for (final Field field : o.getClass().getDeclaredFields()) {
            if (field.getType().equals(Link.class)) {
                try {
                    field.setAccessible(true);
                    final Link<?> link = (Link<?>) field.get(o);
                    final Class<? extends Event> reifiedClass = LambdaUtils.getLambdaTarget(link);
                    if (reifiedClass.equals(e)) {
                        this.classLinkMap.computeIfAbsent(reifiedClass, l -> new PriorityBlockingQueue<>()).offer(new LinkData<>(
                                link,
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
            if (field.getType().equals(Link.class)) {
                try {
                    field.setAccessible(true);
                    final Link<?> link = (Link<?>) field.get(o);
                    final Class<? extends Event> reifiedClass = LambdaUtils.getLambdaTarget(link);
                    if (reifiedClass.equals(e)) {
                        removed |= classLinkMap.get(reifiedClass).removeIf(linkData -> linkData.getLink() == link);
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
    public <T extends Event> Link<T> subscribeDirect(Link<T> link) {
        final Class<? extends Event> reifiedClass = LambdaUtils.getLambdaTarget(link);
        this.classLinkMap.computeIfAbsent(reifiedClass, l -> new PriorityBlockingQueue<>()).offer(new LinkData<>(
                link,
                reifiedClass,
                Priority.NORMAL,
                null)
        );
        return link;
    }

    @Override
    public boolean unsubscribeDirect(Link<?> link) {
        return classLinkMap.get(LambdaUtils.getLambdaTarget(link)).removeIf(linkData -> linkData.getLink() == link);
    }

    @Override
    public boolean subscribeAll(Object o) {
        Objects.requireNonNull(o);
        boolean added = false;
        for (final Field field : o.getClass().getDeclaredFields()) {
            if (field.getType().equals(Link.class)) {
                try {
                    field.setAccessible(true);
                    final Link<?> link = (Link<?>) field.get(o);
                    final Class<? extends Event> reifiedClass = LambdaUtils.getLambdaTarget(link);
                    this.classLinkMap.computeIfAbsent(reifiedClass, l -> new PriorityBlockingQueue<>()).offer(new LinkData<>(
                            link,
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
    public boolean unsubscribeAll(Object o) {
        Objects.requireNonNull(o);
        boolean removed = false;
        for (final Field field : o.getClass().getDeclaredFields()) {
            if (field.getType().equals(Link.class)) {
                try {
                    field.setAccessible(true);
                    removed |= unsubscribeDirect((Link<?>) field.get(o));
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
        final Queue<LinkData> list = classLinkMap.get(event.getClass());
        if (list != null) {
            for (LinkData link : list) {
                final Link<T> castLink = (Link<T>) link.getLink();
                if (link.getFilters() != null) {
                    for (Filter<T> f : link.getFilters()) {
                        if (!f.test(castLink, event)) return event;
                    }
                }
                castLink.invoke(event);
            }
        }
        return event;
    }

    private static Priority findPriority(Field field) {
        final Prioritized priorityAnnotation = field.getAnnotation(Prioritized.class);
        Priority priority = Priority.NORMAL;
        if (priorityAnnotation != null) {
            priority = priorityAnnotation.value();
        }
        return priority;
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

    static class LinkData<T extends Event> implements Comparable<LinkData> {
        private final Link<T> link;
        private final Class<? extends Event> clazz;
        private final Priority priority;
        private final Filter[] filters;

        LinkData(Link<T> link,
                 Class<? extends Event> clazz,
                 Priority priority,
                 Filter[] filters) {
            this.link = link;
            this.clazz = clazz;
            this.priority = priority;
            this.filters = filters;
        }

        Link<T> getLink() {
            return this.link;
        }

        Class<? extends Event> getEventClass() {
            return this.clazz;
        }

        Priority getPriority() {
            return this.priority;
        }

        Filter[] getFilters() {
            return this.filters;
        }

        @Override
        public int compareTo(LinkData o) {
            return Priority.HIGHEST.ordinal() - o.getPriority().ordinal();
        }
    }

}
