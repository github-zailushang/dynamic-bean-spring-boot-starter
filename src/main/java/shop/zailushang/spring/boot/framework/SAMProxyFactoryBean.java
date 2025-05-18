package shop.zailushang.spring.boot.framework;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.NonNull;

@RequiredArgsConstructor
public class SAMProxyFactoryBean<T, R> implements FactoryBean<SAM<T, R>> {
    private final SAM<T, R> target;

    @NonNull
    @Override
    public SAM<T, R> getObject() {
        return target;
    }

    @NonNull
    @Override
    public Class<?> getObjectType() {
        return SAM.class;
    }
}