package me.foundry.lambus.internal.util;

import me.foundry.lambus.internal.SubscriptionData;
import me.foundry.lambus.priority.Priority;

import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Mark Johnson
 */
public final class SubscriberList implements Iterable<SubscriptionData> {

    private final Lock[] NODE_LOCKS = new ReentrantLock[] {
            new ReentrantLock(),
            new ReentrantLock(),
            new ReentrantLock(),
            new ReentrantLock(),
            new ReentrantLock()
    };

    private Node[] priorityHeads, priorityTails;

    public SubscriberList() {
        priorityHeads = new Node[] {null, null, null, null, null};
        priorityTails = new Node[] {null, null, null, null, null};
    }

    public void add(SubscriptionData data) {
        final int idx = data.getPriority().ordinal();
        try {
            NODE_LOCKS[idx].lock();
            linkNode(new Node(data));
        } finally {
            NODE_LOCKS[idx].unlock();
        }
    }

    public boolean removeIf(Predicate<SubscriptionData> predicate) {
        boolean removed = false;
        for (int i = 0; i < Priority.NUM_PRIORITIES; i++) {
            if (priorityHeads[i] != null) {
                try {
                    NODE_LOCKS[i].lock();
                    for (Node curr = priorityHeads[i], pred = null; curr != null; pred = curr, curr = curr.next) {
                        if (predicate.test(curr.data)) {
                            removed = true;
                            unlinkNode(curr, pred);
                        }
                    }
                } finally {
                    NODE_LOCKS[i].unlock();
                }
            }
        }
        return removed;
    }

    private void unlinkNode(Node curr, Node pred) {
        final int idx = curr.getData().getPriority().ordinal();
        if (curr == priorityHeads[idx]) {
            priorityHeads[idx] = curr.next;
            curr.data = null;
        }
        else {
            if (curr == priorityTails[idx]) {
                priorityTails[idx] = pred;
            }
            pred.next = curr.next;
            curr.data = null;
        }
    }

    private void linkNode(Node node) {
        final int idx = node.getData().getPriority().ordinal();
        if (priorityHeads[idx] == null) {
            priorityHeads[idx] = priorityTails[idx] = node;
        }
        else {
            priorityTails[idx].next = node;
            priorityTails[idx] = node;
        }
    }

    @Override
    public void forEach(Consumer<? super SubscriptionData> action) {
        for (int i = 0; i < Priority.NUM_PRIORITIES; i++) {
            if (priorityHeads[i] != null) {
                try {
                    NODE_LOCKS[i].lock();
                    for (Node curr = priorityHeads[i]; curr != null; curr = curr.next) {
                        action.accept(curr.getData());
                    }
                } finally {
                    NODE_LOCKS[i].lock();
                }
            }
        }
    }

    @Override
    public Iterator<SubscriptionData> iterator() {
        return new Itr();
    }

    private static class Node {
        Node next;
        private SubscriptionData data;

        Node(SubscriptionData data) {
            this.data = data;
        }

        SubscriptionData getData() {
            return this.data;
        }
    }

    private class Itr implements Iterator<SubscriptionData> {

        int idx;
        Node curr, pred;

        @Override
        public boolean hasNext() {
            while (curr == null) {
                if (idx == Priority.NUM_PRIORITIES) {
                    return false;
                }
                NODE_LOCKS[idx].lock();
                if (idx > 0) {
                    NODE_LOCKS[idx-1].unlock();
                }
                curr = priorityHeads[idx++];
            }
            return true;
        }

        @Override
        public SubscriptionData next() {
            Node result = pred = curr;
            curr = curr.next;
            return result.getData();
        }
    }
}
