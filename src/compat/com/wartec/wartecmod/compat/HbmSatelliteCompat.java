package com.wartec.wartecmod.compat;

import com.hbm.saveddata.satellites.Satellite;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Registers satellites without linking WarTech to one exact HBM method
 * descriptor. Some HBM forks changed or removed registerSatellite().
 */
public final class HbmSatelliteCompat {
    private HbmSatelliteCompat() {
    }

    public static void register(String satelliteClassName, Item item) {
        try {
            Class<?> loadedClass = Class.forName(satelliteClassName);
            if (!Satellite.class.isAssignableFrom(loadedClass)) {
                throw new IllegalArgumentException(
                        satelliteClassName + " is not an HBM satellite");
            }
            @SuppressWarnings("unchecked")
            Class<? extends Satellite> satelliteClass =
                    (Class<? extends Satellite>) loadedClass;
            register(satelliteClass, item);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "Missing WarTech satellite class " + satelliteClassName,
                    exception);
        }
    }

    public static void register(Class<? extends Satellite> satelliteClass, Item item) {
        if (satelliteClass == null || item == null) {
            throw new IllegalArgumentException("Satellite class and item are required");
        }
        Class<?> registryClass = Satellite.class;
        if (invokeRegistrationMethod(registryClass, satelliteClass, item)) {
            return;
        }
        if (registerThroughFields(registryClass, satelliteClass, item)) {
            return;
        }
        throw new IllegalStateException("Unsupported HBM satellite registry API");
    }

    private static boolean invokeRegistrationMethod(Class<?> registryClass,
            Class<?> satelliteClass, Item item) {
        Method[] methods = registryClass.getDeclaredMethods();
        for (Method method : methods) {
            if (!Modifier.isStatic(method.getModifiers())
                    || !"registerSatellite".equals(method.getName())) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 2) {
                continue;
            }
            Object itemArgument = itemArgument(parameters[1], item);
            boolean classFirst = parameters[0].isAssignableFrom(Class.class)
                    && itemArgument != null;
            Object reverseItemArgument = itemArgument(parameters[0], item);
            boolean classSecond = parameters[1].isAssignableFrom(Class.class)
                    && reverseItemArgument != null;
            if (!classFirst && !classSecond) {
                continue;
            }
            try {
                method.setAccessible(true);
                if (classFirst) {
                    method.invoke(null, satelliteClass, itemArgument);
                } else {
                    method.invoke(null, reverseItemArgument, satelliteClass);
                }
                return true;
            } catch (ReflectiveOperationException ignored) {
                // Try another overload, then fall back to the backing registry.
            } catch (RuntimeException ignored) {
                // Forks may expose an unusable bridge overload.
            } catch (LinkageError ignored) {
                // Its descriptor may reference an optional Space-only type.
            }
        }
        return false;
    }

    private static Object itemArgument(Class<?> parameter, Item item) {
        if (parameter.isAssignableFrom(Item.class)) {
            return item;
        }
        if (parameter.isAssignableFrom(ItemStack.class)) {
            return new ItemStack(item);
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean registerThroughFields(Class<?> registryClass,
            Class<?> satelliteClass, Item item) {
        List satelliteTypes = findNamedList(registryClass, "satellites", "satelliteTypes");
        Map itemMappings = findNamedMap(registryClass, "itemToClass", "satelliteItems");
        for (Field field : registryClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(null);
                if (satelliteTypes == null && value instanceof List) {
                    satelliteTypes = (List) value;
                } else if (itemMappings == null && value instanceof Map) {
                    itemMappings = (Map) value;
                }
            } catch (ReflectiveOperationException ignored) {
                // Keep looking for another compatible backing field.
            } catch (RuntimeException ignored) {
                // Keep looking for another compatible backing field.
            }
        }
        if (satelliteTypes == null || itemMappings == null) {
            return false;
        }
        if (!satelliteTypes.contains(satelliteClass)) {
            satelliteTypes.add(satelliteClass);
        }
        itemMappings.put(item, satelliteClass);
        return true;
    }

    @SuppressWarnings("rawtypes")
    private static List findNamedList(Class<?> registryClass, String... names) {
        for (String name : names) {
            try {
                Field field = registryClass.getDeclaredField(name);
                if (!Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof List) return (List) value;
            } catch (ReflectiveOperationException ignored) {
                // Try the next known field name.
            } catch (RuntimeException ignored) {
                // Try structural discovery below.
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private static Map findNamedMap(Class<?> registryClass, String... names) {
        for (String name : names) {
            try {
                Field field = registryClass.getDeclaredField(name);
                if (!Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof Map) return (Map) value;
            } catch (ReflectiveOperationException ignored) {
                // Try the next known field name.
            } catch (RuntimeException ignored) {
                // Try structural discovery below.
            }
        }
        return null;
    }
}
