package shop.zailushang.spring.boot.service.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import shop.zailushang.spring.boot.model.RefreshBeanModel;
import shop.zailushang.spring.boot.pubsub.event.RefreshBeanEvent;
import shop.zailushang.spring.boot.service.RefreshBeanService;
import shop.zailushang.spring.boot.pubsub.redis.RedisConst;
import shop.zailushang.spring.boot.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@ConditionalOnProperty(name = "dynamic-bean.mode", havingValue = "redis")
public record RefreshBeanServiceRedisImpl(@Qualifier("stringRedisTemplate") RedisTemplate<String, String> redisTemplate,
                                          ApplicationEventPublisher applicationEventPublisher) implements RefreshBeanService {
    @Override
    public List<RefreshBeanModel> selectAll() {
        var jsonStrList = redisTemplate.<String, String>opsForHash()
                .entries(RedisConst.REFRESH_BEAN_KEY)
                .values();
        return parseList(jsonStrList);
    }

    @Override
    public int insert(RefreshBeanModel refreshBeanModel) {
        var beanName = refreshBeanModel.beanName();
        var hashOperations = redisTemplate.<String, String>opsForHash();
        Assert.isFalse(RedisConst.REFRESH_BEAN_KEY, beanName, hashOperations::hasKey, () -> new IllegalArgumentException("model already exists"));
        hashOperations.put(RedisConst.REFRESH_BEAN_KEY, beanName, refreshBeanModel.toJson());
        refresh(RefreshBeanEvent.addWith(refreshBeanModel));
        return 1;
    }

    @Override
    public int update(RefreshBeanModel refreshBeanModel) {
        var beanName = refreshBeanModel.beanName();
        var hashOperations = redisTemplate.<String, String>opsForHash();
        Assert.isTrue(RedisConst.REFRESH_BEAN_KEY, beanName, hashOperations::hasKey, () -> new NoSuchElementException("model not exists"));
        var beforeModel = RefreshBeanModel.parse(hashOperations.get(RedisConst.REFRESH_BEAN_KEY, beanName));
        hashOperations.put(RedisConst.REFRESH_BEAN_KEY, beanName, refreshBeanModel.toJson());
        var afterModel = RefreshBeanModel.parse(hashOperations.get(RedisConst.REFRESH_BEAN_KEY, beanName));
        // 修改了 lambdaScript 时，才触发后续的刷新操作
        if (beforeModel.diff(afterModel)) refresh(RefreshBeanEvent.updateWith(beforeModel, refreshBeanModel));
        return 1;
    }

    @Override
    public int delete(String beanName) {
        var hashOperations = redisTemplate.<String, String>opsForHash();
        Assert.isTrue(RedisConst.REFRESH_BEAN_KEY, beanName, hashOperations::hasKey, () -> new NoSuchElementException("model not exists"));
        hashOperations.delete(RedisConst.REFRESH_BEAN_KEY, beanName);
        refresh(RefreshBeanEvent.deleteWith(RefreshBeanModel.withBeanName(beanName)));
        return 1;
    }

    private List<RefreshBeanModel> parseList(Collection<String> jsonList) {
        return jsonList.stream().map(RefreshBeanModel::parse).toList();
    }

    private void refresh(RefreshBeanEvent refreshBeanEvent) {
        applicationEventPublisher.publishEvent(refreshBeanEvent);
    }
}
