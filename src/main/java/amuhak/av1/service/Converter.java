package amuhak.av1.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class Converter {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Converter.class);

    @Value("${thread.pool.size}")
    private int maximumPoolSize;

    private static ThreadPoolExecutor executor;

    public static ConcurrentHashMap<String, AtomicBoolean> conversionStatus = new ConcurrentHashMap<>();

    @PostConstruct
    private void initializeExecutor() {
        if (executor == null) {
            executor = new ThreadPoolExecutor(1, maximumPoolSize, 5, TimeUnit.MINUTES, new LinkedBlockingDeque<>());
            logger.info("Thread Pool has max size of: {}", maximumPoolSize);
        }
    }

    public static void addJob(Job job) {
        if (executor != null) {
            executor.execute(job);
        } else {
            logger.error("Executor not initialized. Cannot add job.");
        }
    }
}
