package shop.zailushang.spring.boot.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.function.Function;

@Configuration
public class RedisModeAutoConfiguration {
    // redis模式下资源配置
    @Slf4j
    @Configuration
    @RequiredArgsConstructor
    @AutoConfigureAfter(EarlySourceRegistrar.class)
    @ConditionalOnProperty(name = "dynamic-bean.mode", havingValue = "redis")
    static class RedisModeSourceRegistrar {
        // BeanDefinition 注册器
        @Bean
        public static BeanDefinitionRegistryPostProcessor beanDefinitionRegistryPostProcessor(Environment environment, @Qualifier("groovyGetter") Function<ClassLoader, ScriptEngine> scriptEngineGetter, RefreshableScope refreshableScope) {
            return registry -> {
                log.info("starting RedisMode BeanDefinitionRegistry.");
                var beanDefinitionHolders = RefreshableBeanDefinitionResolver.resolveBeanDefinitionFromRedis(environment, scriptEngineGetter, refreshableScope);
                beanDefinitionHolders.forEach(beanDefinitionHolder -> registry.registerBeanDefinition(beanDefinitionHolder.getBeanName(), beanDefinitionHolder.getBeanDefinition()));
            };
        }
    }

    // redis模式下配置 事件监听器
    @EnableAsync
    @Configuration
    @RequiredArgsConstructor
    @AutoConfigureAfter(EarlySourceRegistrar.class)
    @ConditionalOnProperty(name = "dynamic-bean.mode", havingValue = "redis")
    static class RedisModeListenerRegistrar {
        private final DefaultListableBeanFactory defaultListableBeanFactory;
        private final RefreshableScope refreshableScope;
        private final Function<ClassLoader, ScriptEngine> scriptEngineGetter;

        @Async
        @TransactionalEventListener(RefreshBeanEvent.class)
        public void eventListener(RefreshBeanEvent refreshBeanEvent) {
            new DefaultEventProcessor(defaultListableBeanFactory, refreshableScope, scriptEngineGetter)
                    .processEvent(refreshBeanEvent);
        }
    }
}
