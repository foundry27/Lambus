package me.foundry.lambus.internal;

import me.foundry.lambus.Lambus;
import me.foundry.lambus.Link;
import me.foundry.lambus.event.Event;
import me.foundry.lambus.filter.Filter;
import me.foundry.lambus.filter.Filtered;
import me.foundry.lambus.priority.Prioritized;
import me.foundry.lambus.priority.Priority;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by Mark on 1/24/2016.
 */
public final class SynchronousLambus implements Lambus {
    private final Map<Class<? extends Event>, PriorityLinkDataList> classLinkMap = new ConcurrentHashMap<>();

    /*
    Public Methods
     */
    @Override
    public boolean subscribe(Class<? extends Event> e, Object o) {
        Objects.requireNonNull(o);
        boolean added = false;
        for (final Field field : o.getClass().getDeclaredFields()) {
            if (field.getType().equals(Link.class)) {
                try {
                    field.setAccessible(true);
                    final Link<?> link = (Link<?>) field.get(o);
                    final Class<? extends Event> reifiedClass = getLambdaTarget(link);
                    if (reifiedClass.equals(e)) {
                        this.classLinkMap.computeIfAbsent(reifiedClass, l -> new PriorityLinkDataList()).add(new LinkData<>(
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
    public <T extends Event> Link<T> subscribeDirect(Link<T> link) {
        final Class<? extends Event> reifiedClass = getLambdaTarget(link);
        this.classLinkMap.computeIfAbsent(reifiedClass, l -> new PriorityLinkDataList()).add(new LinkData<>(link, reifiedClass, Priority.NORMAL, null));
        return link;
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
                    final Class<? extends Event> reifiedClass = getLambdaTarget(link);
                    this.classLinkMap.computeIfAbsent(reifiedClass, l -> new PriorityLinkDataList()).add(new LinkData<>(
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
//                    TODO: Only try to iterate through the sublist corresponding to the priority of the Link
                } catch (IllegalAccessException | SecurityException ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        }
        return removed;
    }

    @Override
    public boolean unsubscribeDirect(Link<?> link) {
        boolean removed = false;
        final Class<? extends Event> reifiedClass = getLambdaTarget(link);
        for (Iterator<LinkData> it = classLinkMap.getOrDefault(reifiedClass, PriorityLinkDataList.EMPTY).iterator(); it.hasNext();) {
            if (it.next().getLink() == link) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event> T post(T event) {
        Objects.requireNonNull(event);
        for (LinkData<? extends Event> link : this.classLinkMap.getOrDefault(event.getClass(), PriorityLinkDataList.EMPTY)) {
            final Link<T> castLink = (Link<T>) link.getLink();
            if (link.getFilters() != null) {
                for (Filter<T> f : link.getFilters()) {
                    if (!f.test(castLink, event)) {
                        return event;
                    }
                }
            }
            castLink.invoke(event);
        }
        return event;
    }

    /*
    Static Methods
     */
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

    @SuppressWarnings("unchecked")
    private static <T extends Event> Class<T> getLambdaTarget(Link<T> link) {
        return (Class<T>) getLambdaMethod(getSerializedLambda(link)).getParameterTypes()[0];
    }

    private static SerializedLambda getSerializedLambda(Object function) {
        if (function == null || !(function instanceof java.io.Serializable))
            throw new IllegalArgumentException();

        for (Class<?> clazz = function.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                final Method replaceMethod = clazz.getDeclaredMethod("writeReplace");
                replaceMethod.setAccessible(true);
                Object serializedForm = replaceMethod.invoke(function);

                if (serializedForm instanceof SerializedLambda)
                    return (SerializedLambda) serializedForm;
            }
            catch (NoSuchMethodError e) {
                /* fall through the loop and try the next class */
            }
            catch (Throwable t) {
                throw new RuntimeException("Error while extracting serialized lambda", t);
            }
        }

        throw new RuntimeException("writeReplace method not found");
    }

    private static Method getLambdaMethod(SerializedLambda lambda) {
        Class<?> implClass; String implClassName = lambda.getImplClass().replace('/', '.');
        try {
            implClass = Class.forName(implClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to instantiate lambda class");
        }

        final String lambdaName = lambda.getImplMethodName();

        for (Method m : implClass.getDeclaredMethods()) {
            if (m.getName().equals(lambdaName)) {
                return m;
            }
        }

        throw new RuntimeException("Lambda Method not found");
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

    static class PriorityLinkDataList implements Iterable<LinkData> {
//        TODO: Concurrent modification protection
        public static PriorityLinkDataList EMPTY = new PriorityLinkDataList();

        Priority lowPriority, highPriority;

        final SubList[] priorityLists = new SubList[] {
                new SubList(), //HIGHEST
                new SubList(), //HIGH
                new SubList(), //NORMAL
                new SubList(), //LOW
                new SubList()  //LOWEST
        };

        public void add(LinkData link) {
            final int index = link.getPriority().ordinal();
            final LinkedLink prev = priorityLists[index].head.previous;
            priorityLists[index].head.previous = new LinkedLink(link);
            priorityLists[index].head.previous.next = priorityLists[index].head;
            priorityLists[index].head.previous.previous = prev;
            prev.next = priorityLists[index].head.previous;
            if (lowPriority == null) {
                /* both must be null */
                lowPriority = highPriority = link.getPriority();
            }
            else {
                if (link.getPriority().ordinal() > lowPriority.ordinal()) {
                    lowPriority = link.getPriority();
                }
                else if (link.getPriority().ordinal() < highPriority.ordinal()) {
                    highPriority = link.getPriority();
                }
            }
        }

        @Override
        public Iterator<LinkData> iterator() {
            return new LinkDataIterator();
        }

        static class LinkedLink {
            LinkedLink next, previous;
            final LinkData data;
            public LinkedLink(LinkData link) {
                this.data = link;
            }
        }

        static class SubList {
            final LinkedLink tail = new LinkedLink(null), head = new LinkedLink(null);
            public SubList() {
                this.tail.next = this.head;
                this.head.previous = this.tail;
            }
        }

        class LinkDataIterator implements Iterator<LinkData> {
            int priorityIndex = highPriority != null ? highPriority.ordinal() : Priority.HIGHEST.ordinal();
            SubList currentList = priorityLists[priorityIndex];
            LinkedLink currentLink = currentList.tail.next;

            @Override
            public boolean hasNext() {
                if (lowPriority == null) return false;
                if (currentLink == currentList.head && priorityIndex != lowPriority.ordinal()) {
                    currentList = priorityLists[++priorityIndex];
                    currentLink = currentList.tail.next;
                    return hasNext();
                }
                else {
                    return currentLink != currentList.head;
                }
            }

            @Override
            public LinkData next() {
                final LinkedLink result = currentLink;
                currentLink = currentLink.next;
                return result.data;
            }

            @Override
            public void remove() {
                final LinkedLink prev = currentLink.previous;
                prev.previous.next = currentLink;
                currentLink.previous = prev.previous;
                if (currentList.tail.next == currentList.head) {
                    /* no more Links of that priority */

                    Priority newPriority = null;
                    if (prev.data.getPriority().ordinal() == lowPriority.ordinal()) {
                        for (int i = lowPriority.ordinal() - 1; i > highPriority.ordinal(); i--) {
                            if (priorityLists[i].tail.next != priorityLists[i].head)
                                newPriority = Priority.fromOrdinal[i];
                            priorityIndex = i;
                        }
                        lowPriority = newPriority == null ? highPriority = null : newPriority;
                    }
                    else if (prev.data.getPriority().ordinal() == highPriority.ordinal()) {
                        for (int i = highPriority.ordinal() + 1; i < lowPriority.ordinal(); i++) {
                            if (priorityLists[i].tail.next != priorityLists[i].head)
                                newPriority = Priority.fromOrdinal[i];
                            priorityIndex = i;
                        }
                        highPriority = newPriority == null ? lowPriority = null : newPriority;
                    }
                }
            }
        }
    }

}
