package me.foundry.lambus.internal;

import me.foundry.lambus.Event;
import me.foundry.lambus.Lambus;
import me.foundry.lambus.Link;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by Mark on 1/24/2016.
 */
public final class SynchronousLambus implements Lambus {
    private final Map<Class<? extends Event>, List<Link<?>>> classLinkMap= new HashMap<>();

    @Override
    public boolean subscribe(Class<? extends Event> e, Object o) {
        for (final Field field : o.getClass().getDeclaredFields()) {
            if (field.getType().equals(Link.class)) {
                try {
                    field.setAccessible(true);
                    final Link<?> link = (Link<?>) field.get(o);
                    @SuppressWarnings("unchecked")
                    final Class<? extends Event> reifiedClass = (Class<? extends Event>) getLambdaMethod(getSerializedLambda(link)).getParameterTypes()[0];
                    if (reifiedClass.equals(e))
                        this.classLinkMap.computeIfAbsent(reifiedClass, l -> new ArrayList<>()).add(link);
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean subscribeAll(Object o) {
        for (final Field field : o.getClass().getDeclaredFields()) {
            if (field.getType().equals(Link.class)) {
                try {
                    field.setAccessible(true);
                    final Link<?> link = (Link<?>) field.get(o);
                    @SuppressWarnings("unchecked")
                    final Class<? extends Event> reifiedClass = (Class<? extends Event>) getLambdaMethod(getSerializedLambda(link)).getParameterTypes()[0];
                    this.classLinkMap.computeIfAbsent(reifiedClass, l -> new ArrayList<>()).add(link);
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean unsubscribeAll(Object o) {
        for (final Field field : o.getClass().getDeclaredFields()) {
            if (field.getType().equals(Link.class)) {
                try {
                    field.setAccessible(true);
                    final Link<?> link = (Link<?>) field.get(o);
                    @SuppressWarnings("unchecked")
                    final Class<? extends Event> reifiedClass = (Class<? extends Event>) getLambdaMethod(getSerializedLambda(link)).getParameterTypes()[0];
                    this.classLinkMap.remove(reifiedClass);
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public <T extends Event> T post(T event) {
        for (Link<?> link : this.classLinkMap.getOrDefault(event.getClass(), Collections.emptyList())) {
            @SuppressWarnings("unchecked")
            final Link<T> castLink = (Link<T>) link;
            castLink.invoke(event);
        }
        return event;
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
                // fall through the loop and try the next class
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

}
