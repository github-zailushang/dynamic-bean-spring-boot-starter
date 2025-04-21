package shop.zailushang.spring.boot.util;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 断言工具类
 */
@SuppressWarnings("unused")
public class Assert {

    private Assert() {
    }

    /**
     * @param t          待校验参数
     * @param verifier   参数校验器
     * @param exSupplier 异常构造器
     * @param <T>        校验类型
     * @param <E>        异常类型
     *                   满足性校验：使用 verifier 传入待校验参数 t,要求参数必须满足校验条件
     *                   (必须返回 true,当返回 false 时则抛出 exSupplier 制造的异常)
     */
    public static <T, E extends RuntimeException> void isTrue(T t, Predicate<T> verifier, Supplier<E> exSupplier) {
        Objects.requireNonNull(verifier, "verifier is null");
        Objects.requireNonNull(exSupplier, "exSupplier is null");
        // 断言结果
        boolean asserted = verifier.test(t);
        // 要求断言必须通过
        ifNotThrow(asserted, exSupplier);
    }

    /**
     * @param t          待校验参数
     * @param verifier   参数校验器
     * @param exSupplier 异常构造器
     * @param <T>        校验类型
     * @param <E>        异常类型
     *                   不满足性校验：使用 verifier 传入待校验参数 t,要求参数必须不满足校验条件
     *                   (必须返回 false,当返回 true 时则抛出 exSupplier 制造的异常)
     */
    public static <T, E extends RuntimeException> void isFalse(T t, Predicate<T> verifier, Supplier<E> exSupplier) {
        Objects.requireNonNull(verifier, "verifier is null");
        Objects.requireNonNull(exSupplier, "exSupplier is null");
        // 断言结果
        boolean asserted = verifier.test(t);
        // 要求断言必须不通过
        ifThrow(asserted, exSupplier);
    }

    /**
     * @param left       待校验参数1
     * @param right      待校验参数2
     * @param verifier   参数校验器
     * @param exSupplier 异常构造器
     * @param <T>        校验参数类型1
     * @param <U>        校验参数类型2
     * @param <E>        异常类型
     *                   满足性校验：使用 verifier 传入待校验参数 left,right,要求参数必须满足校验条件
     *                   (必须返回 false,当返回 true 时则抛出 exSupplier 制造的异常)
     */
    public static <T, U, E extends RuntimeException> void isTrue(T left, U right, BiPredicate<T, U> verifier, Supplier<E> exSupplier) {
        Objects.requireNonNull(verifier, "verifier is null");
        Objects.requireNonNull(exSupplier, "exSupplier is null");
        // 断言结果
        boolean asserted = verifier.test(left, right);
        // 要求断言必须通过
        ifNotThrow(asserted, exSupplier);
    }

    /**
     * @param left       待校验参数1
     * @param right      待校验参数2
     * @param verifier   参数校验器
     * @param exSupplier 异常构造器
     * @param <T>        校验参数类型1
     * @param <U>        校验参数类型2
     * @param <E>        异常类型
     *                   不满足性校验：使用 verifier 传入待校验参数 left,right,要求参数必须满足校验条件
     *                   (必须返回 false,当返回 true 时则抛出 exSupplier 制造的异常)
     */
    public static <T, U, E extends RuntimeException> void isFalse(T left, U right, BiPredicate<T, U> verifier, Supplier<E> exSupplier) {
        Objects.requireNonNull(verifier, "verifier is null");
        Objects.requireNonNull(exSupplier, "exSupplier is null");
        // 断言结果
        boolean asserted = verifier.test(left, right);
        // 要求断言必须不通过
        ifThrow(asserted, exSupplier);
    }

    /********************************************** 快速抛出异常的方法 **********************************************/
    public static <E extends RuntimeException> void ifThrow(boolean asserted, Supplier<E> exSupplier) {
        if (asserted) throw exSupplier.get();
    }

    public static <E extends RuntimeException> void ifNotThrow(boolean asserted, Supplier<E> exSupplier) {
        ifThrow(!asserted, exSupplier);
    }

    /********************************************** 以下为断言时的常用方法,提供静态方法引用 **********************************************/
    // 对象为空
    public static <T> boolean isNull(T t) {
        return Objects.isNull(t);
    }

    // 对象非空
    public static <T> boolean isNotNull(T t) {
        return Objects.nonNull(t);
    }

    // str 非空
    public static boolean strNotBlank(String str) {
        return isNotNull(str) && !str.isBlank();
    }

    // 集合非空
    public static <T> boolean collectionNotEmpty(Collection<T> collection) {
        return isNotNull(collection) && !collection.isEmpty();
    }

    // 数组非空
    public static <T> boolean arrayNotEmpty(T[] array) {
        return isNotNull(array) && array.length > 0;
    }

    // str 正则匹配
    public static boolean strRgx(String str, String regex) {
        return isNotNull(str)
                && isNotNull(regex)
                && Pattern.matches(regex, str);
    }

    // 对象相等
    public static <T, U> boolean isEq(T left, U right) {
        return Objects.equals(left, right);
    }

    // 对象不相等
    public static <T, U> boolean isNotEq(T left, U right) {
        return !isEq(left, right);
    }

    // 比较后相等,不做判空校验，逻辑不允许
    public static <T extends Comparable<T>> boolean isOrderEq(T caller, T reference) {
        return caller.compareTo(reference) == 0;
    }

    // 比较后不相等,不做判空校验，逻辑不允许
    public static <T extends Comparable<T>> boolean isOrderNotEq(T caller, T reference) {
        return !isOrderEq(caller, reference);
    }

    // 比较后大于,不做判空校验，逻辑不允许
    public static <T extends Comparable<T>> boolean isOrderGt(T caller, T reference) {
        return caller.compareTo(reference) > 0;
    }

    // 比较后大于等于,不做判空校验，逻辑不允许
    public static <T extends Comparable<T>> boolean isOrderGe(T caller, T reference) {
        return caller.compareTo(reference) >= 0;
    }

    // 比较后小于,不做判空校验，逻辑不允许
    public static <T extends Comparable<T>> boolean isOrderLt(T caller, T reference) {
        return caller.compareTo(reference) < 0;
    }

    // 比较后小于等于,不做判空校验，逻辑不允许
    public static <T extends Comparable<T>> boolean isOrderLe(T caller, T reference) {
        return caller.compareTo(reference) <= 0;
    }
}