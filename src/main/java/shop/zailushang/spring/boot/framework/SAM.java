package shop.zailushang.spring.boot.framework;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

// 单一抽象方法
@FunctionalInterface
public interface SAM<T, R> extends Runnable, Consumer<T>, Supplier<R>, Function<T, R>, Predicate<T> {

    @Override
    default void run() {
        execute(null);
    }

    @Override
    default void accept(T param) {
        execute(param);
    }

    @Override
    default R get() {
        return execute(null);
    }

    @Override
    default R apply(T param) {
        return execute(param);
    }

    @Override
    default boolean test(T param) {
        return (boolean) execute(param);
    }

    // 添加Spring MVC 注解，为 Dynamic Controller 做预留
    @ResponseBody
    R execute(@RequestBody T param);
}