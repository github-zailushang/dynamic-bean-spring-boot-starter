package shop.zailushang.spring.boot.service.impl;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import shop.zailushang.spring.boot.model.RefreshBeanModel;
import shop.zailushang.spring.boot.service.RefreshBeanService;
import shop.zailushang.spring.boot.pubsub.redis.RedisConst;
import shop.zailushang.spring.boot.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@ConditionalOnProperty(name = "dynamic-bean.mode", havingValue = "redis")
public record RefreshBeanServiceRedisImpl(RedissonClient redissonClient) implements RefreshBeanService {
    @Override
    public List<RefreshBeanModel> selectAll() {
        var jsonStrList = redissonClient.<String, String>getMapCache(RedisConst.REFRESH_BEAN_KEY)
                .values();
        return parseList(jsonStrList);
    }

    @Override
    public int insert(RefreshBeanModel refreshBeanModel) {
        var beanName = refreshBeanModel.beanName();
        var rMapCache = redissonClient.<String, String>getMapCache(RedisConst.REFRESH_BEAN_KEY);
        Assert.isFalse(beanName, rMapCache::containsKey, () -> new IllegalArgumentException("model already exists"));
        rMapCache.fastPut(beanName, refreshBeanModel.toJson());
        return 1;
    }

    @Override
    public int update(RefreshBeanModel refreshBeanModel) {
        var beanName = refreshBeanModel.beanName();
        var rMapCache = redissonClient.<String, String>getMapCache(RedisConst.REFRESH_BEAN_KEY);
        Assert.isTrue(beanName, rMapCache::containsKey, () -> new NoSuchElementException("model not exists"));
        rMapCache.fastPut(beanName, refreshBeanModel.toJson());
        return 1;
    }

    @Override
    public int delete(String beanName) {
        var rMapCache = redissonClient.<String, String>getMapCache(RedisConst.REFRESH_BEAN_KEY);
        Assert.isTrue(beanName, rMapCache::containsKey, () -> new NoSuchElementException("model not exists"));
        rMapCache.fastRemove(beanName);
        return 1;
    }

    private List<RefreshBeanModel> parseList(Collection<String> jsonList) {
        return jsonList.stream().map(RefreshBeanModel::parse).toList();
    }
}
