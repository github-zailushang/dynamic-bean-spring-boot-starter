package shop.zailushang.spring.boot.framework;

import groovy.lang.GroovyClassLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.NonNull;

import java.lang.reflect.Proxy;

@RequiredArgsConstructor
public class SAMProxyFactoryBean<T, R> implements FactoryBean<SAM<T, R>> {

    private final SAM<T, R> target;
    // 缓存代理实例
    private volatile SAM<T, R> proxy;

    @NonNull
    @Override
    public SAM<T, R> getObject() {
        if (proxy == null) {
            // FactoryBean 的 this 已在 scope 中缓存，所以这里使用 this 作为锁对象无碍
            synchronized (this) {
                if (proxy == null) proxy = createProxy();
            }
        }
        return proxy;
    }

    // 使用 jdk 动态代理生成代理对象
    @SuppressWarnings("unchecked")
    private SAM<T, R> createProxy() {
        return (SAM<T, R>) Proxy.newProxyInstance(
                new GroovyClassLoader(),// 使用自定义 ClassLoader，用完即抛，防止内存泄露
                new Class[]{SAM.class},
                // 这里如果不存在额外的逻辑的话，生成 Proxy 实属多余操作
                // 这里创建代理，实际是为了后续可能发生的变化做预留
                (proxy, method, args) -> method.invoke(target, args));
    }

    @NonNull
    @Override
    public Class<?> getObjectType() {
        return SAM.class;
    }
}
