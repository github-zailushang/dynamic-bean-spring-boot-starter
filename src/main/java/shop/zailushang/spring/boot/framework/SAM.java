package shop.zailushang.spring.boot.framework;

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

    R execute(T param);
}