package shop.zailushang.spring.boot.service.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
public class RefreshBeanServiceRedisImpl implements RefreshBeanService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    public RefreshBeanServiceRedisImpl(@Qualifier("stringRedisTemplate") RedisTemplate<String, String> redisTemplate, ApplicationEventPublisher applicationEventPublisher) {
        this.redisTemplate = redisTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public List<RefreshBeanModel> selectAll() {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        var jsonStrList = hashOperations.entries(RedisConst.REFRESH_BEAN_KEY).values();
        return parseList(jsonStrList);
    }

    @Override
    @Transactional
    public int insert(RefreshBeanModel refreshBeanModel) {
        var beanName = refreshBeanModel.beanName();
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        Assert.isFalse(RedisConst.REFRESH_BEAN_KEY, beanName, hashOperations::hasKey, () -> new IllegalArgumentException("model already exists"));
        hashOperations.put(RedisConst.REFRESH_BEAN_KEY, beanName, refreshBeanModel.toJson());
        refresh(RefreshBeanEvent.addWith(refreshBeanModel));
        return 1;
    }

    @Override
    @Transactional
    public int update(RefreshBeanModel refreshBeanModel) {
        var beanName = refreshBeanModel.beanName();
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        Assert.isTrue(RedisConst.REFRESH_BEAN_KEY, beanName, hashOperations::hasKey, () -> new NoSuchElementException("model not exists"));
        var beforeModel = RefreshBeanModel.parse(hashOperations.get(RedisConst.REFRESH_BEAN_KEY, beanName));
        hashOperations.put(RedisConst.REFRESH_BEAN_KEY, beanName, refreshBeanModel.toJson());
        refresh(RefreshBeanEvent.updateWith(beforeModel, refreshBeanModel));
        return 1;
    }

    @Override
    @Transactional
    public int delete(String beanName) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
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
