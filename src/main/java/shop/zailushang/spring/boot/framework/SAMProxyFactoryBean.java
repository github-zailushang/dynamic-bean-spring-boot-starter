package shop.zailushang.spring.boot.framework;

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
                SAM.class.getClassLoader(),
                new Class[]{SAM.class},
                (proxy, method, args) -> method.invoke(target, args));
    }

    @NonNull
    @Override
    public Class<?> getObjectType() {
        return SAM.class;
    }
}
