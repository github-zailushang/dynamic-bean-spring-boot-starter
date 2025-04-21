package shop.zailushang.spring.boot.pubsub.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import shop.zailushang.spring.boot.model.RefreshBeanModel;
import shop.zailushang.spring.boot.framework.RefreshableScope;
import shop.zailushang.spring.boot.util.Assert;
import shop.zailushang.spring.boot.util.RefreshableBeanDefinitionResolver;

import javax.script.ScriptEngine;
import java.util.NoSuchElementException;

@Slf4j
@RequiredArgsConstructor
public class DefaultEventProcessor implements EventProcessor {
    private final DefaultListableBeanFactory defaultListableBeanFactory;

    private final RefreshableScope refreshableScope;

    private final ScriptEngine scriptEngine;

    @Override
    public void processEvent(RefreshBeanEvent refreshBeanEvent) {
        // 更新时已使用 LinkedHashMap，保证先消费 DEL，后消费 ADD
        refreshBeanEvent.getSource()
                .forEach((eventType, refreshBeanModel) -> {
                    switch (eventType) {
                        case DEL -> del(refreshBeanModel);
                        case ADD -> add(refreshBeanModel);
                        default -> throw new IllegalArgumentException("unknown event type");
                    }
                });
    }

    // 新增时，注册 BeanDefinition
    private void add(RefreshBeanModel refreshBeanModel) {
        var beanName = refreshBeanModel.beanName();
        var beanDefinition = defaultListableBeanFactory.getBeanDefinition(beanName);
        Assert.isTrue(beanDefinition, Assert::isNull, () -> new IllegalArgumentException("beanDefinition already exists"));
        var beanDefinitionHolder = RefreshableBeanDefinitionResolver.resolveBeanDefinitionFromModel(refreshBeanModel, scriptEngine, refreshableScope);
        defaultListableBeanFactory.registerBeanDefinition(beanDefinitionHolder.getBeanName(), beanDefinitionHolder.getBeanDefinition());
        log.info("add beanDefinition: {}", beanDefinitionHolder.getBeanName());
    }

    // 删除时，删除 Bean实例, 并删除 BeanDefinition。
    private void del(RefreshBeanModel refreshBeanModel) {
        var beanName = refreshBeanModel.beanName();
        var beanDefinition = defaultListableBeanFactory.getBeanDefinition(beanName);
        Assert.isTrue(beanDefinition, Assert::isNotNull, () -> new NoSuchElementException("beanDefinition not found"));
        refreshableScope.remove(refreshBeanModel.beanName());
        defaultListableBeanFactory.removeBeanDefinition(refreshBeanModel.beanName());
        log.info("del beanDefinition: {}", beanName);
    }
}
