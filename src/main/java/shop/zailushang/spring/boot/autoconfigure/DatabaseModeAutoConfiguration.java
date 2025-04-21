package shop.zailushang.spring.boot.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.zailushang.spring.boot.framework.RefreshableScope;
import shop.zailushang.spring.boot.pubsub.event.DefaultEventProcessor;
import shop.zailushang.spring.boot.pubsub.event.RefreshBeanEvent;
import shop.zailushang.spring.boot.util.RefreshableBeanDefinitionResolver;

import javax.script.ScriptEngine;

@Configuration
public class DatabaseModeAutoConfiguration {
    // 数据库模式下资源配置
    @Slf4j
    @Configuration
    @AutoConfigureAfter(EarlySourceRegistrar.class)
    @ConditionalOnProperty(name = "dynamic-bean.mode", havingValue = "database")
    static class DatabaseModeSourceRegistrar {
        // BeanDefinition 注册器
        @Bean
        public static BeanDefinitionRegistryPostProcessor beanDefinitionRegistryPostProcessor(Environment environment, ScriptEngine scriptEngine, RefreshableScope refreshableScope) {
            return registry -> {
                log.info("starting DatabaseMode BeanDefinitionRegistry.");
                var beanDefinitionHolders = RefreshableBeanDefinitionResolver.resolveBeanDefinitionFromDatabase(environment, scriptEngine, refreshableScope);
                beanDefinitionHolders.forEach(beanDefinitionHolder -> registry.registerBeanDefinition(beanDefinitionHolder.getBeanName(), beanDefinitionHolder.getBeanDefinition()));
            };
        }
    }

    // 数据库模式下配置 事件监听器
    @EnableAsync
    @Configuration
    @RequiredArgsConstructor
    @AutoConfigureAfter(EarlySourceRegistrar.class)
    @ConditionalOnProperty(name = "dynamic-bean.mode", havingValue = "database")
    static class DatabaseModeListenerRegistrar {
        private final DefaultListableBeanFactory defaultListableBeanFactory;
        private final RefreshableScope refreshableScope;
        private final ScriptEngine scriptEngine;

        @Async
        @TransactionalEventListener(RefreshBeanEvent.class)
        public void eventListener(RefreshBeanEvent refreshBeanEvent) {
            new DefaultEventProcessor(defaultListableBeanFactory, refreshableScope, scriptEngine)
                    .processEvent(refreshBeanEvent);
        }
    }
}
