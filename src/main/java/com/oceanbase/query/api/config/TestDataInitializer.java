package com.oceanbase.query.api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * 测试数据初始化配置
 * 用于在开发和测试环境中自动加载测试数据
 */
@Configuration
@Profile({"dev", "test"}) // 仅在开发和测试环境中启用
public class TestDataInitializer {

    private static final Logger logger = Logger.getLogger(TestDataInitializer.class.getName());

    /**
     * 创建命令行运行器，在应用启动时自动初始化测试数据
     * @param dataSource 数据源
     * @return CommandLineRunner 实例
     */
    @Bean
    public CommandLineRunner initTestData(@Autowired DataSource dataSource) {
        return args -> {
            logger.info("开始初始化测试数据...");
            
            try {
                // 使用 Spring 的 ResourceDatabasePopulator 执行 SQL 脚本
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("test-data.sql"));
                populator.execute(dataSource);
                
                logger.info("测试数据初始化完成！");
            } catch (Exception e) {
                logger.severe("测试数据初始化失败: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
    
    /**
     * 手动初始化测试数据的方法，可以在需要时调用
     * @param dataSource 数据源
     */
    public static void manualInitTestData(DataSource dataSource) {
        try {
            logger.info("开始手动初始化测试数据...");
            
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("test-data.sql"));
            populator.execute(dataSource);
            
            logger.info("手动测试数据初始化完成！");
        } catch (Exception e) {
            logger.severe("手动测试数据初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
