package me.foundry.lambus.internal.util;

import me.foundry.lambus.Link;
import me.foundry.lambus.event.Event;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

/**
 * @author Mark Johnson
 */

public final class LambdaUtils {
    private LambdaUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends Event> Class<T> getLambdaTarget(Link<T> link) {
        return (Class<T>) getLambdaMethod(getSerializedLambda(link)).getParameterTypes()[0];
    }

    public static SerializedLambda getSerializedLambda(Object function) {
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

    public static Method getLambdaMethod(SerializedLambda lambda) {
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
