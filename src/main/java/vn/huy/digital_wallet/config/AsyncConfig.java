package vn.huy.digital_wallet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig { // Cấu hình thread pool ở đây

    @Bean(name = "walletExecutor")
    public Executor walletExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 5 nhân viên biên chế
        executor.setMaxPoolSize(20); // cao điểm là 20
        executor.setQueueCapacity(100); // hàng chờ tô đa 100
        executor.setThreadNamePrefix("walletExecutor-"); // đặt tên cho dễ đọc log
        executor.initialize();
        return executor;
    }

}
