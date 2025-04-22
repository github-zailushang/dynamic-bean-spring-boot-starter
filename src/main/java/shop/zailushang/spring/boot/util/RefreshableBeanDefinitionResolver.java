package shop.zailushang.spring.boot.util;

import groovy.lang.GroovyClassLoader;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import shop.zailushang.spring.boot.framework.SAMProxyFactoryBean;
import shop.zailushang.spring.boot.model.RefreshBeanModel;
import shop.zailushang.spring.boot.framework.RefreshableScope;
import shop.zailushang.spring.boot.pubsub.redis.RedisConst;

import javax.script.ScriptEngine;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class RefreshableBeanDefinitionResolver {
    // 从数据库中获取所有需要动态注册的Bean定义
    public static Set<BeanDefinitionHolder> resolveBeanDefinitionFromDatabase(Environment environment, Function<ClassLoader, ScriptEngine> scriptEngineGetter, RefreshableScope refreshableScope) {
        var jdbcTemplate = resolverEarlyJdbcTemplate(environment);
        var refreshBeanList = jdbcTemplate.query(
                "select * from refresh_bean",
                (rs, rowNum) -> new RefreshBeanModel(
                        rs.getLong("id"),
                        rs.getString("bean_name"),
                        rs.getString("lambda_script"),
                        rs.getString("description")
                ));
        return refreshBeanList.stream()
                .map(refreshBeanModel -> resolveBeanDefinitionFromModel(refreshBeanModel, scriptEngineGetter, refreshableScope))
                .peek(beanDefinitionHolder -> log.debug("register beanDefinition, {} => {}", beanDefinitionHolder.getBeanName(), beanDefinitionHolder.getBeanDefinition()))
                .collect(Collectors.toSet());
    }

    // 从Redis中获取所有需要动态注册的Bean定义
    public static Set<BeanDefinitionHolder> resolveBeanDefinitionFromRedis(Environment environment, Function<ClassLoader, ScriptEngine> scriptEngineGetter, RefreshableScope refreshableScope) {
        try (var redisClient = resolverEarlyRedisClient(environment); var connect = redisClient.connect()) {
            return connect.sync()
                    .hgetall(RedisConst.REFRESH_BEAN_KEY)
                    .values()
                    .stream()
                    .map(RefreshBeanModel::parse)
                    .map(refreshBeanModel -> resolveBeanDefinitionFromModel(refreshBeanModel, scriptEngineGetter, refreshableScope))
                    .peek(beanDefinitionHolder -> log.debug("register beanDefinition, {} => {}", beanDefinitionHolder.getBeanName(), beanDefinitionHolder.getBeanDefinition()))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 根据 RefreshBeanModel 创建 BeanDefinitionHolder
    public static BeanDefinitionHolder resolveBeanDefinitionFromModel(RefreshBeanModel refreshBeanModel, Function<ClassLoader, ScriptEngine> scriptEngineGetter, RefreshableScope refreshableScope) {
        var lambdaScript = refreshBeanModel.lambdaScript();
        var beanName = refreshBeanModel.beanName();
        // 每个动态类使用唯一的 ClassLoader 加载，用完即抛，防止内存泄露
        try (var classLoader = new GroovyClassLoader()) {
            var scriptEngine = scriptEngineGetter.apply(classLoader);
            var target = scriptEngine.eval(lambdaScript);
            // 注册Bean定义
            var beanDefinition = BeanDefinitionBuilder
                    .genericBeanDefinition(SAMProxyFactoryBean.class)
                    .addConstructorArgValue(target)
                    .setScope(refreshableScope.name())
                    .getBeanDefinition();
            return new BeanDefinitionHolder(beanDefinition, beanName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 获取 Early JdbcTemplate
    private static JdbcTemplate resolverEarlyJdbcTemplate(Environment environment) {
        log.debug("Starting to access the early datasource.");
        var url = environment.getProperty("spring.datasource.url");
        var username = environment.getProperty("spring.datasource.username");
        var password = environment.getProperty("spring.datasource.password", "");
        var dataSource = new DriverManagerDataSource(url, username, password);
        // 连接数据库，加载配置类
        return new JdbcTemplate(dataSource);
    }

    // 获取 Early RedisClient
    private static RedisClient resolverEarlyRedisClient(Environment environment) {
        log.debug("Starting to access the early redis.");
        var host = environment.getProperty("spring.data.redis.host");
        var port = environment.getProperty("spring.data.redis.port", Integer.class);
        var password = environment.getProperty("spring.data.redis.password");
        var builder = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withDatabase(0);
        if (Assert.strNotBlank(password)) builder.withPassword((CharSequence) password);
        var redisUri = builder.build();
        return RedisClient.create(redisUri);
    }
}