package shop.zailushang.spring.boot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import shop.zailushang.spring.boot.model.RefreshBeanModel;
import shop.zailushang.spring.boot.pubsub.redis.RedisConst;
import shop.zailushang.spring.boot.util.Assert;

import java.util.List;

public class RedisTestDataInit {
    // init redis test data before start application
    @Test
    void initRedisData() throws Exception {
        // address and password
        var address = "redis://192.168.33.128:6379";
        // if used password set password here
        var password = "";

        var url = ClassLoader.getSystemClassLoader()
                .getResource("dynamic_bean.json");
        var refreshBeanList = new ObjectMapper().readValue(url, new TypeReference<List<RefreshBeanModel>>() {
        });

        var config = new Config();
        // use  string codec
        config.setCodec(StringCodec.INSTANCE);
        var singleServerConfig = config.useSingleServer();
        singleServerConfig.setAddress(address);
        if (Assert.strNotBlank(password)) singleServerConfig.setPassword(password);
        var redissonClient = Redisson.create(config);
        try {
            var rMapCache = redissonClient.<String, String>getMapCache(RedisConst.REFRESH_BEAN_KEY, StringCodec.INSTANCE);
            rMapCache.clear();
            refreshBeanList.forEach(refreshBeanModel -> rMapCache.fastPut(refreshBeanModel.beanName(), refreshBeanModel.toJson()));
        } finally {
            redissonClient.shutdown();
        }
    }
}