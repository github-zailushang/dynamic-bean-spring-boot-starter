package shop.zailushang.spring.boot.framework;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 自定义域对象，存储 RefreshAble Bean
@RequiredArgsConstructor
public class RefreshableScope implements Scope {

    private final DefaultListableBeanFactory defaultListableBeanFactory;

    private final Map<String, FactoryBean<SAM<?, ?>>> factoryBeanCache = new ConcurrentHashMap<>();
    private final Map<String, Runnable> destructionCallbackCache = new ConcurrentHashMap<>();

    public String name() {
        return "REFRESHABLE_SCOPE";
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public FactoryBean<SAM<?, ?>> get(@NonNull String name, @NonNull ObjectFactory<?> objectFactory) {
        factoryBeanCache.computeIfAbsent(name, key -> {
            // 创建工厂Bean时，同步生成销毁回调
            registerDestructionCallback(name, () -> defaultListableBeanFactory.removeBeanDefinition(name));
            // 创建并缓存工厂Bean
            return (FactoryBean<SAM<?, ?>>) objectFactory.getObject();
        });
        return factoryBeanCache.get(name);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Object remove(@NonNull String name) {
        // 从未调用过 getBean 方法，直接删除时
        if (!factoryBeanCache.containsKey(name)) {
            defaultListableBeanFactory.removeBeanDefinition(name);
            return null;
        }
        // 调用过 getBean 方法
        factoryBeanCache.remove(name);
        destructionCallbackCache.remove(name).run();
        return null;
    }

    public void registerDestructionCallback(@NonNull String name, @NonNull Runnable callback) {
        destructionCallbackCache.putIfAbsent(name, callback);
    }

    @NonNull
    @Override
    public Object resolveContextualObject(@NonNull String key) {
        return "";
    }

    @NonNull
    @Override
    public String getConversationId() {
        return "";
    }
}
