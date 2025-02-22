package com.betting.systemmanager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.betting.Application;

/**
 * check system load and set systemOverloaded flag
 */
public class SystemMonitor implements Runnable {
    private static final double CPU_THRESHOLD = 1.0; // CPU使用率阈值
    private static final double MEM_THRESHOLD = 0.9; // 80% 内存使用率阈值

    private static Logger logger = LoggerFactory.getLogger(SystemMonitor.class);

    // do't need to syncronize as only one thread is reading and writing
    private static long lastTimestamp = 0;
    private static long lastUsage = 0;

    @Override
    public void run() {
        try {
            // get CPU load
            double cpuUsageRatio = this.getCpuUseRatioInCgroupv2();
            double memoryUsageRatio = this.getMemoryUseRatioInCgroupV2();

            // check if system is overloaded
            boolean overloaded = cpuUsageRatio > CPU_THRESHOLD || memoryUsageRatio > MEM_THRESHOLD;

            Application.systemOverloaded.set(overloaded);

            logger.info("System load: cpuUsageRatio={}, memoryUsageRatio={}, overloaded={}", cpuUsageRatio,
                    memoryUsageRatio, overloaded);

        } catch (Exception e) {
            logger.error("System monitor error", e);
            Application.systemOverloaded.set(false);
        }
    }

    private double getMemoryUseRatioInCgroupV2() {
        double memoryUsageRatio = -1;
        try {

            // 获取内存使用量
            long usage = readLongFromFile("/sys/fs/cgroup/memory.current");

            // 获取内存限制
            String memoryMax = readFile("/sys/fs/cgroup/memory.max");
            long limit = memoryMax.equals("max") ? -1 : Long.parseLong(memoryMax);

            // 计算内存使用占比
            if (limit > 0) {
                memoryUsageRatio = ((double) usage / limit);
            }
        } catch (IOException e) {
            logger.error("Failed to read memory usage: " + e.getMessage());
        }
        return memoryUsageRatio;
    }

    private double getCpuUseRatioInCgroupv2() {

        double cpuUsageRatio = 0;

        try {
            // 获取 CPU 使用时间
            long usage = readCpuUsage("/sys/fs/cgroup/cpu.stat");

            // 获取 CPU 配额和周期
            String[] cpuMax = readFile("/sys/fs/cgroup/cpu.max").split(" ");
            long quota = cpuMax[0].equals("max") ? -1 : Long.parseLong(cpuMax[0]);
            long period = Long.parseLong(cpuMax[1]);

            long currentTimestamp = System.nanoTime() / 1000;

            long timeInterval = currentTimestamp - lastTimestamp;

            // 计算 CPU 使用占比
            if (lastUsage > 0 && quota > 0 && period > 0) {
                long deltaUsage = usage - lastUsage;
                double cpuLimit = (double) quota / period;
                cpuUsageRatio = (deltaUsage / (cpuLimit * timeInterval));
            }

            // 更新上一次的 CPU 使用时间
            lastUsage = usage;
            lastTimestamp = currentTimestamp;
        } catch (IOException e) {
            logger.error("Failed to read CPU usage: " + e.getMessage());
        }
        return cpuUsageRatio;
    }

    private static long readLongFromFile(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            return Long.parseLong(line.trim());
        }
    }

    private static long readCpuUsage(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("usage_usec")) {
                    return Long.parseLong(line.split(" ")[1]);
                }
            }
            throw new IOException("usage_usec not found in " + filePath);
        }
    }

    private static String readFile(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            return reader.readLine();
        }
    }
}
