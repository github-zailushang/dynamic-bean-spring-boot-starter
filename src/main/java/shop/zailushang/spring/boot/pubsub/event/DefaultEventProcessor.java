package shop.zailushang.spring.boot.pubsub.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import shop.zailushang.spring.boot.framework.ScriptEngineCreator;
import shop.zailushang.spring.boot.model.RefreshBeanModel;
import shop.zailushang.spring.boot.framework.RefreshableScope;
import shop.zailushang.spring.boot.util.Assert;
import shop.zailushang.spring.boot.util.RefreshableBeanDefinitionResolver;

@Slf4j
@RequiredArgsConstructor
public class DefaultEventProcessor implements EventProcessor {
    private final DefaultListableBeanFactory defaultListableBeanFactory;

    private final RefreshableScope refreshableScope;

    private final ScriptEngineCreator scriptEngineCreator;

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
        try {
            var beanName = refreshBeanModel.beanName();
            var beanDefinition = defaultListableBeanFactory.getBeanDefinition(beanName);
            // 存在时，抛出 IllegalArgumentException 异常
            Assert.isTrue(beanDefinition, Assert::isNull, () -> new IllegalArgumentException("beanDefinition already exists"));
        } catch (NoSuchBeanDefinitionException e) {
            // 当 BeanDefinition 不存在时
            var beanDefinitionHolder = RefreshableBeanDefinitionResolver.resolveBeanDefinitionFromModel(refreshBeanModel, scriptEngineCreator, refreshableScope);
            var beanName = beanDefinitionHolder.getBeanName();
            var beanDefinition = beanDefinitionHolder.getBeanDefinition();
            // 注册 BeanDefinition
            defaultListableBeanFactory.registerBeanDefinition(beanName, beanDefinition);
            log.info("add beanDefinition: {}", beanName);
        }
    }

    // 删除时，删除 Bean实例, 并删除 BeanDefinition。
    private void del(RefreshBeanModel refreshBeanModel) {
        var beanName = refreshBeanModel.beanName();
        // 移除 Bean 实例 自动触发销毁回调
        refreshableScope.remove(beanName);
        log.info("del beanDefinition: {}", beanName);
    }
}
