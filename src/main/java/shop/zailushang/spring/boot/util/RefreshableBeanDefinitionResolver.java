package shop.zailushang.spring.boot.util;

import groovy.lang.GroovyClassLoader;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import shop.zailushang.spring.boot.framework.SAMProxyFactoryBean;
import shop.zailushang.spring.boot.framework.ScriptEngineCreator;
import shop.zailushang.spring.boot.model.RefreshBeanModel;
import shop.zailushang.spring.boot.framework.RefreshableScope;
import shop.zailushang.spring.boot.pubsub.redis.RedisConst;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class RefreshableBeanDefinitionResolver {
    // 从数据库中获取所有需要动态注册的Bean定义
    public static Set<BeanDefinitionHolder> resolveBeanDefinitionFromDatabase(Environment environment, ScriptEngineCreator scriptEngineCreator, RefreshableScope refreshableScope) {
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
                .map(refreshBeanModel -> resolveBeanDefinitionFromModel(refreshBeanModel, scriptEngineCreator, refreshableScope))
                .peek(beanDefinitionHolder -> log.debug("register beanDefinition from database, {} => {}", beanDefinitionHolder.getBeanName(), beanDefinitionHolder.getBeanDefinition()))
                .collect(Collectors.toSet());
    }

    // 从Redis中获取所有需要动态注册的Bean定义
    public static Set<BeanDefinitionHolder> resolveBeanDefinitionFromRedis(Environment environment, ScriptEngineCreator scriptEngineCreator, RefreshableScope refreshableScope) {
        var redissonClient = resolverEarlyRedissonClient(environment);
        var rMapCache = redissonClient.<String, String>getMapCache(RedisConst.REFRESH_BEAN_KEY);
        try {
            return rMapCache
                    .values()
                    .stream()
                    .map(RefreshBeanModel::parse)
                    .map(refreshBeanModel -> resolveBeanDefinitionFromModel(refreshBeanModel, scriptEngineCreator, refreshableScope))
                    .peek(beanDefinitionHolder -> log.debug("register beanDefinition from redis, {} => {}", beanDefinitionHolder.getBeanName(), beanDefinitionHolder.getBeanDefinition()))
                    .collect(Collectors.toSet());
        } finally {
            redissonClient.shutdown();
        }
    }

    // 根据 RefreshBeanModel 创建 BeanDefinitionHolder
    public static BeanDefinitionHolder resolveBeanDefinitionFromModel(RefreshBeanModel refreshBeanModel, ScriptEngineCreator scriptEngineCreator, RefreshableScope refreshableScope) {
        var lambdaScript = refreshBeanModel.lambdaScript();
        var beanName = refreshBeanModel.beanName();
        // 每个动态类使用唯一的 ClassLoader 加载，用完即抛，防止内存泄露
        try (var classLoader = new GroovyClassLoader()) {
            var scriptEngine = scriptEngineCreator.createScriptEngine(classLoader);
            var target = scriptEngine.eval(lambdaScript);
            // 生成 Bean定义
            var beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(SAMProxyFactoryBean.class)
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
        var password = environment.getProperty("spring.datasource.password");
        var dataSource = new DriverManagerDataSource(url, username, password);
        return new JdbcTemplate(dataSource);
    }

    // 获取 Early RedissonClient
    private static RedissonClient resolverEarlyRedissonClient(Environment environment) {
        log.debug("Starting to access the early redis.");
        try {
            var config = Config.fromYAML(environment.getProperty("spring.redis.redisson.config"));
            return Redisson.create(config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}