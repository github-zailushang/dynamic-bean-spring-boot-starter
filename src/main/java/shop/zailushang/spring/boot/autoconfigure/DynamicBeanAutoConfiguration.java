package shop.zailushang.spring.boot.autoconfigure;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Import;
import shop.zailushang.spring.boot.controller.RefreshBeanController;
import shop.zailushang.spring.boot.controller.SamplesController;
import shop.zailushang.spring.boot.service.impl.RefreshBeanServiceDatabaseImpl;
import shop.zailushang.spring.boot.service.impl.RefreshBeanServiceRedisImpl;

// use autoconfig import
@Import({EarlySourceRegistrar.class, DatabaseModeAutoConfiguration.class, DatabaseAutoModeAutoConfiguration.class, RedisModeAutoConfiguration.class,
        SamplesController.class, RefreshBeanController.class, RefreshBeanServiceDatabaseImpl.class, RefreshBeanServiceRedisImpl.class,})
@MapperScan("shop.zailushang.spring.boot.mapper")
public class DynamicBeanAutoConfiguration {
}
