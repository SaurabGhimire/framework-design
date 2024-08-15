package framework.scheduled;

import framework.Framework;
import framework.annotations.Scheduled;
import framework.exceptions.InstanceCreationWrapperException;
import framework.exceptions.InvalidCronStringFormat;
import framework.utils.CheckForAnnotation;
import framework.utils.CronUtility;
import framework.utils.PropertyAccessor;
import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduling {
    private static final int DEFAULT_THREAD_POOL_SIZE = 5;
    private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(getThreadPoolSize());

    public static void trigger(Collection<Class<?>> serviceAnnotatedClasses)
            throws InvalidCronStringFormat, InstanceCreationWrapperException {
        for (Class<?> clazz : serviceAnnotatedClasses) {
            List<Method> scheduledMethods = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(CheckForAnnotation::hasScheduled)
                    .toList();

            Object classInstance = Framework.getInstanceFromAppContext(clazz, true);

            scheduleMethods(classInstance, scheduledMethods);
        }
    }

    private static int getThreadPoolSize() {
        String poolSizeString = (String) PropertyAccessor.getValueOf("scheduling.pool.size");
        int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;

        try {
            if (!Strings.isEmpty(poolSizeString)) {
                threadPoolSize = Integer.parseInt(poolSizeString);
            }
        } catch (NumberFormatException e) {
            System.out.println("Ignoring `scheduling.pool.size` setting in properties file since it's " +
                    "not a valid number/integer. Found: " + poolSizeString);
        }

        return threadPoolSize;
    }

    private static void scheduleMethods(Object classInstance, List<Method> methods) throws InvalidCronStringFormat {
        for (Method method : methods) {
            long fixedRate = method.getAnnotation(Scheduled.class).fixedRate();
            String cron = method.getAnnotation(Scheduled.class).cron();
            long secondsToSchedule = 0;
            if (fixedRate > 0L) {
                secondsToSchedule = fixedRate / 1000;
            } else {
                secondsToSchedule = CronUtility.parseTotalSecondsFromCron(cron);
            }

            scheduleMethod(classInstance, method, secondsToSchedule);
        }
    }

    private static void scheduleMethod(Object classInstance, Method method, long secondsToSchedule) {
        Runnable methodRunnable = () -> {
            try {
                method.invoke(classInstance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };

        scheduledExecutorService.scheduleAtFixedRate(methodRunnable, 0, secondsToSchedule, TimeUnit.SECONDS);
    }
}
