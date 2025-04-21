package shop.zailushang.spring.boot.autoconfigure;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.event.TransactionalEventListener;
import shop.zailushang.spring.boot.framework.RefreshableScope;
import shop.zailushang.spring.boot.pubsub.database.CanalClientListener;
import shop.zailushang.spring.boot.pubsub.database.CanalConfigurationProperties;
import shop.zailushang.spring.boot.pubsub.event.DefaultEventProcessor;
import shop.zailushang.spring.boot.pubsub.event.RefreshBeanEvent;
import shop.zailushang.spring.boot.util.RefreshableBeanDefinitionResolver;

import javax.script.ScriptEngine;
import java.net.InetSocketAddress;

@Configuration
public class DatabaseAutoModeAutoConfiguration {
    // 数据库自动模式下资源配置
    @Slf4j
    @Configuration
    @EnableConfigurationProperties(CanalConfigurationProperties.class)
    @ConditionalOnProperty(name = "dynamic-bean.mode", havingValue = "database-auto")
    static class DatabaseAutoModeSourceRegistrar {
        // BeanDefinition 注册器
        @Bean
        public static BeanDefinitionRegistryPostProcessor beanDefinitionRegistryPostProcessor(Environment environment, ScriptEngine scriptEngine, RefreshableScope refreshableScope) {
            return registry -> {
                log.info("starting DatabaseAutoMode BeanDefinitionRegistry.");
                var beanDefinitionHolders = RefreshableBeanDefinitionResolver.resolveBeanDefinitionFromDatabase(environment, scriptEngine, refreshableScope);
                beanDefinitionHolders.forEach(beanDefinitionHolder -> registry.registerBeanDefinition(beanDefinitionHolder.getBeanName(), beanDefinitionHolder.getBeanDefinition()));
            };
        }

        // canal 客户端
        @Bean
        public CanalConnector canalConnector(CanalConfigurationProperties canalConfigurationProperties) {
            var canalConnector = CanalConnectors.newSingleConnector(
                    new InetSocketAddress(canalConfigurationProperties.getServerHost(), canalConfigurationProperties.getServerPort()),
                    canalConfigurationProperties.getDestination(),
                    canalConfigurationProperties.getUsername(),
                    canalConfigurationProperties.getPassword()
            );
            canalConnector.connect();
            canalConnector.subscribe(canalConfigurationProperties.getSubscribeFilter());
            return canalConnector;
        }
    }

    // 数据库自动模式下配置 事件监听器
    @EnableAsync
    @Configuration
    @RequiredArgsConstructor
    @AutoConfigureAfter(EarlySourceRegistrar.class)
    @ConditionalOnProperty(name = "dynamic-bean.mode", havingValue = "database-auto")
    static class DatabaseAutoModeListenerRegistrar {
        private final CanalConnector canalConnector;
        private final ApplicationEventPublisher applicationEventPublisher;
        private final DefaultListableBeanFactory defaultListableBeanFactory;
        private final RefreshableScope refreshableScope;
        private final ScriptEngine scriptEngine;

        @Async
        @EventListener(ApplicationReadyEvent.class)
        public void canalListener() {
            new CanalClientListener(canalConnector, applicationEventPublisher)
                    .startListener();
        }

        @Async
        @TransactionalEventListener(RefreshBeanEvent.class)
        public void eventListener(RefreshBeanEvent refreshBeanEvent) {
            new DefaultEventProcessor(defaultListableBeanFactory, refreshableScope, scriptEngine)
                    .processEvent(refreshBeanEvent);
        }
    }
}
