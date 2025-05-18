package shop.zailushang.spring.boot.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.api.map.event.EntryCreatedListener;
import org.redisson.api.map.event.EntryRemovedListener;
import org.redisson.api.map.event.EntryUpdatedListener;
import org.redisson.api.map.event.MapEntryListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import shop.zailushang.spring.boot.framework.RefreshableScope;
import shop.zailushang.spring.boot.framework.ScriptEngineCreator;
import shop.zailushang.spring.boot.model.RefreshBeanModel;
import shop.zailushang.spring.boot.pubsub.event.DefaultEventProcessor;
import shop.zailushang.spring.boot.pubsub.event.RefreshBeanEvent;
import shop.zailushang.spring.boot.pubsub.redis.RedisConst;
import shop.zailushang.spring.boot.util.RefreshableBeanDefinitionResolver;

import java.util.Set;

@Configuration
public class RedisModeAutoConfiguration {
    // redis模式下资源配置
    @Slf4j
    @Configuration
    @RequiredArgsConstructor
    @AutoConfigureAfter(EarlySourceRegistrar.class)
    @ConditionalOnProperty(name = "dynamic-bean.mode", havingValue = "redis")
    static class RedisModeSourceRegistrar {

        private final ApplicationEventPublisher applicationEventPublisher;

        @Bean
        public EntryCreatedListener<String, String> entryCreatedListener() {
            return event -> {
                var key = event.getKey();
                refresh(RefreshBeanEvent.addWith(RefreshBeanModel.parse(event.getValue())));
                log.info("redis listener create callback ==> {}", key);
            };
        }

        @Bean
        public EntryUpdatedListener<String, String> entryUpdatedListener() {
            return event -> {
                var key = event.getKey();
                var oldValue = event.getOldValue();
                var value = event.getValue();
                var beforeModel = RefreshBeanModel.parse(oldValue);
                var afterModel = RefreshBeanModel.parse(value);
                // 修改了 lambdaScript 时，才触发后续的刷新操作
                if (beforeModel.diff(afterModel)) refresh(RefreshBeanEvent.updateWith(beforeModel, afterModel));
                log.info("redis listener update callback ==> {}", key);
            };
        }

        @Bean
        public EntryRemovedListener<String, String> entryRemovedListener() {
            return event -> {
                var key = event.getKey();
                refresh(RefreshBeanEvent.deleteWith(RefreshBeanModel.withBeanName(key)));
                log.info("redis listener delete callback ==> {}", key);
            };
        }

        // BeanDefinition 注册器
        @Bean
        public static BeanDefinitionRegistryPostProcessor beanDefinitionRegistryPostProcessor(Environment environment, @Qualifier("groovyCreator") ScriptEngineCreator scriptEngineCreator, RefreshableScope refreshableScope) {
            return registry -> {
                log.info("starting RedisMode BeanDefinitionRegistry.");
                var beanDefinitionHolders = RefreshableBeanDefinitionResolver.resolveBeanDefinitionFromRedis(environment, scriptEngineCreator, refreshableScope);
                beanDefinitionHolders.forEach(beanDefinitionHolder -> registry.registerBeanDefinition(beanDefinitionHolder.getBeanName(), beanDefinitionHolder.getBeanDefinition()));
            };
        }

        private void refresh(RefreshBeanEvent refreshBeanEvent) {
            applicationEventPublisher.publishEvent(refreshBeanEvent);
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
        private final ScriptEngineCreator scriptEngineCreator;

        private final RedissonClient redissonClient;
        private final Set<MapEntryListener> mapEntryListeners;

        @Async
        @EventListener(ApplicationReadyEvent.class)
        public void eventListener() {
            var rMapCache = redissonClient.<String, String>getMapCache(RedisConst.REFRESH_BEAN_KEY);
            // 注册监听器
            mapEntryListeners.forEach(rMapCache::addListener);
        }

        @Async
        @EventListener(RefreshBeanEvent.class)
        public void eventListener(RefreshBeanEvent refreshBeanEvent) {
            new DefaultEventProcessor(defaultListableBeanFactory, refreshableScope, scriptEngineCreator)
                    .processEvent(refreshBeanEvent);
        }
    }
}
