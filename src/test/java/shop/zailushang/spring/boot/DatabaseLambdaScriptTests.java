package shop.zailushang.spring.boot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SpringBootTest
@SuppressWarnings("unchecked")
public class DatabaseLambdaScriptTests {
    @Autowired
    private ApplicationContext applicationContext;

    // 测试 Runnable 类型任务 任务id 1
    @Test
    void runnableTest() {
        var runnable = applicationContext.getBean("runnable-task", Runnable.class);
        runnable.run();
    }

    // 测试 Consumer 类型任务 任务id 2
    @Test
    void consumerTest() {
        var consumer = applicationContext.getBean("consumer-task", Consumer.class);
        var name = "zailushang";
        consumer.accept(name);
    }

    // 测试 Supplier 类型任务 任务id 3
    @Test
    void supplierTest() {
        Supplier<String> supplier = applicationContext.getBean("supplier-task", Supplier.class);
        var name = supplier.get();
        System.out.println("your name is " + name);
    }

    // 测试 Function 类型任务 任务id 4
    @Test
    void functionTest() {
        Function<String, String> function = applicationContext.getBean("function-task", Function.class);
        var no = "make PHP great again.";
        var yes = function.apply(no);
        System.out.println(yes);
    }

    // 测试 Predicate 类型任务 任务id 5
    @Test
    void predicateTest() {
        Predicate<String> predicate = applicationContext.getBean("predicate-task", Predicate.class);
        var gender = "gay";
        boolean isBadGender = predicate.test(gender);
        System.out.println("is a bad gender? " + (isBadGender ? "yes" : "no"));
    }

    // 测试 使用act查找依赖 任务id 6
    @Test
    void injectTest() {
        var runnable = applicationContext.getBean("run-4-act", Runnable.class);
        runnable.run();
    }

    // 测试 使用InheritableThreadLocal查找依赖 任务id 7
    @Test
    void threadLocalTest() {
        var inheritableThreadLocal = applicationContext.getBean(InheritableThreadLocal.class);
        inheritableThreadLocal.set("zailushang");
        System.out.println("before : " + inheritableThreadLocal.get());
        var runnable = applicationContext.getBean("run-4-itl", Runnable.class);
        runnable.run();
        System.out.println("after : " + inheritableThreadLocal.get());
    }
}
