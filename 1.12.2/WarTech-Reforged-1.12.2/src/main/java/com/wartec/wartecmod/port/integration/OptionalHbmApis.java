package com.wartec.wartecmod.port.integration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Reflection-only probes for HBM branches whose APIs are not shared by NTM Extended.
 */
public final class OptionalHbmApis {
    private static final String[] ARTILLERY_ENTITY_CLASSES = {
        "com.hbm.entity.projectile.EntityArtilleryShell",
        "com.hbm.entity.projectile.EntityArtilleryRocket"
    };
    private static final String[] SATELLITE_BASE_CLASSES = {
        "com.hbm.saveddata.satellites.Satellite"
    };
    private static final String[] SATELLITE_DATA_CLASSES = {
        "com.hbm.saveddata.satellites.SatelliteSavedData",
        "com.hbm.saveddata.SatelliteSavedData"
    };

    private OptionalHbmApis() {
    }

    public static boolean hasArtilleryApi() {
        return findClass(ARTILLERY_ENTITY_CLASSES) != null;
    }

    public static boolean isArtilleryEntity(Object candidate) {
        return isInstanceOfAny(candidate, ARTILLERY_ENTITY_CLASSES);
    }

    public static boolean hasSatelliteApi() {
        return findClass(SATELLITE_BASE_CLASSES) != null
            && findClass(SATELLITE_DATA_CLASSES) != null;
    }

    public static boolean isSatelliteType(Class<?> candidate) {
        Class<?> satelliteBase = findClass(SATELLITE_BASE_CLASSES);
        return candidate != null
            && satelliteBase != null
            && satelliteBase.isAssignableFrom(candidate);
    }

    public static Class<?> findClass(String... classNames) {
        if (classNames == null) {
            return null;
        }

        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader fallbackLoader = OptionalHbmApis.class.getClassLoader();
        for (String className : classNames) {
            if (className == null || className.isEmpty()) {
                continue;
            }
            Class<?> found = tryLoad(className, contextLoader);
            if (found == null && fallbackLoader != contextLoader) {
                found = tryLoad(className, fallbackLoader);
            }
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static boolean isInstanceOfAny(Object candidate, String... classNames) {
        if (candidate == null || classNames == null) {
            return false;
        }
        for (String className : classNames) {
            Class<?> type = findClass(className);
            if (type != null && type.isInstance(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns null when the class or compatible static method is absent.
     * Invocation failures are wrapped so genuine API errors are not mistaken for absence.
     */
    public static Object invokeStaticIfPresent(
        String[] classNames,
        String methodName,
        Class<?>[] parameterTypes,
        Object... arguments
    ) {
        Class<?> owner = findClass(classNames);
        if (owner == null || methodName == null) {
            return null;
        }

        try {
            Method method = owner.getDeclaredMethod(
                methodName,
                parameterTypes == null ? new Class<?>[0] : parameterTypes
            );
            if (!Modifier.isStatic(method.getModifiers())) {
                return null;
            }
            method.setAccessible(true);
            return method.invoke(null, arguments == null ? new Object[0] : arguments);
        } catch (NoSuchMethodException exception) {
            return null;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access optional HBM API " + owner.getName() + "." + methodName, exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IllegalStateException("Optional HBM API failed " + owner.getName() + "." + methodName, cause);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Cannot invoke optional HBM API " + owner.getName() + "." + methodName, exception);
        } catch (LinkageError error) {
            return null;
        }
    }

    private static Class<?> tryLoad(String className, ClassLoader classLoader) {
        if (classLoader == null) {
            return null;
        }
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException exception) {
            return null;
        } catch (LinkageError error) {
            return null;
        } catch (SecurityException exception) {
            return null;
        }
    }
}
