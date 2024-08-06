package amuhak.av1.service;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Converter {
    static ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 5, TimeUnit.MINUTES, new LinkedBlockingDeque<>());

    public static void addJob(Job job) {
        executor.execute(job);
    }
}
