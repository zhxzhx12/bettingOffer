package com.betting;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * check system load and set systemOverloaded flag
 */
public class SystemMonitor implements Runnable {
    private static final double CPU_THRESHOLD = 0.8; // 80% CPU使用率阈值
    private static final double MEM_THRESHOLD = 0.8; // 80% 内存使用率阈值

    @Override
    public void run() {
        try {
            // get cup load
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemLoadAverage() / osBean.getAvailableProcessors();

            // get momory usage
            Runtime runtime = Runtime.getRuntime();
            double usedMem = runtime.totalMemory() - runtime.freeMemory();
            double memRatio = usedMem / runtime.maxMemory();

            // check if system is overloaded
            boolean overloaded = cpuLoad > CPU_THRESHOLD || memRatio > MEM_THRESHOLD;
            Application.systemOverloaded.set(overloaded);

        } catch (Exception e) {
            e.printStackTrace();
            Application.systemOverloaded.set(false);
        }
    }
}
