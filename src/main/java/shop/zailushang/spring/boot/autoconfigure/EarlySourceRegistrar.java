package shop.zailushang.spring.boot.autoconfigure;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import shop.zailushang.spring.boot.framework.RefreshableScope;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

// 提前初始化的资源配置
@Configuration
public class EarlySourceRegistrar {
    // 自定义可刷新作用域对象：refreshableScope
    @Bean("refreshableScope")
    public static RefreshableScope refreshableScope() {
        return new RefreshableScope();
    }

    // 配置使用自定义作用域对象
    @Bean
    @DependsOn("refreshableScope")
    public static BeanFactoryPostProcessor beanFactoryPostProcessor(RefreshableScope refreshableScope) {
        return beanFactory -> beanFactory.registerScope(refreshableScope.name(), refreshableScope);
    }

    // groovy 脚本环境下绑定此 InheritableThreadLocal 进行个性化参数传递
    @Bean("inheritableThreadLocal")
    public static InheritableThreadLocal<Object> inheritableThreadLocal() {
        return new InheritableThreadLocal<>();
    }

    // groovy 脚本引擎
    @Bean("groovy")
    @DependsOn("inheritableThreadLocal")
    public static ScriptEngine scriptEngine(ApplicationContext applicationContext, @Qualifier("inheritableThreadLocal") InheritableThreadLocal<Object> inheritableThreadLocal) {
        var scriptEngineManager = new ScriptEngineManager();
        var groovy = scriptEngineManager.getEngineByName("groovy");
        var context = groovy.getContext();
        // 绑定上下文对象
        context.setAttribute("act", applicationContext, ScriptContext.ENGINE_SCOPE);
        context.setAttribute("itl", inheritableThreadLocal, ScriptContext.ENGINE_SCOPE);
        return groovy;
    }
}