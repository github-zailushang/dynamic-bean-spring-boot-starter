package shop.zailushang.spring.boot.framework;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RefreshableScope implements Scope {

    private final Map<String, FactoryBean<SAM<?, ?>>> factoryBeanCache = new ConcurrentHashMap<>();

    public String name() {
        return "REFRESHABLE_SCOPE";
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public FactoryBean<SAM<?, ?>> get(@NonNull String name, @NonNull ObjectFactory<?> objectFactory) {
        return factoryBeanCache.computeIfAbsent(name, key -> (FactoryBean<SAM<?, ?>>) objectFactory.getObject());
    }

    @NonNull
    @Override
    public Object remove(@NonNull String name) {
        return factoryBeanCache.remove(name);
    }

    public void registerDestructionCallback(@NonNull String name, @NonNull Runnable callback) {
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
