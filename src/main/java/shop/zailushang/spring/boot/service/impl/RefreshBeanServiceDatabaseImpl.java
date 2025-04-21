package shop.zailushang.spring.boot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.zailushang.spring.boot.mapper.RefreshBeanMapper;
import shop.zailushang.spring.boot.model.RefreshBeanModel;
import shop.zailushang.spring.boot.pubsub.event.RefreshBeanEvent;
import shop.zailushang.spring.boot.service.RefreshBeanService;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dynamic-bean.mode", havingValue = "database")
public class RefreshBeanServiceDatabaseImpl implements RefreshBeanService {

    private final ApplicationEventPublisher applicationEventPublisher;

    private final RefreshBeanMapper refreshBeanMapper;

    @Override
    public List<RefreshBeanModel> selectAll() {
        return refreshBeanMapper.selectAll();
    }

    @Override
    @Transactional
    public int insert(RefreshBeanModel refreshBeanModel) {
        var beanName = refreshBeanModel.beanName();
        refreshBeanMapper.selectOne(beanName).ifPresent(rbm -> {
            throw new IllegalArgumentException("model already exists");
        });
        var result = refreshBeanMapper.insert(refreshBeanModel);
        refresh(RefreshBeanEvent.addWith(refreshBeanModel));
        return result;
    }

    @Override
    @Transactional
    public int update(RefreshBeanModel refreshBeanModel) {
        var beforeModel = refreshBeanMapper.selectOne(refreshBeanModel.beanName())
                .orElseThrow(() -> new NoSuchElementException("model not found"));
        var result = refreshBeanMapper.update(refreshBeanModel);
        var afterModel = refreshBeanMapper.selectOne(refreshBeanModel.beanName())
                .orElseThrow(() -> new NoSuchElementException("model not found"));
        // 当修改了 lambdaScript 时，才触发后续的刷新操作
        if (beforeModel.diff(afterModel)) refresh(RefreshBeanEvent.updateWith(beforeModel, afterModel));
        return result;
    }

    @Override
    @Transactional
    public int delete(String beanName) {
        refreshBeanMapper.selectOne(beanName).orElseThrow(() -> new NoSuchElementException("model not found"));
        var result = refreshBeanMapper.delete(beanName);
        refresh(RefreshBeanEvent.deleteWith(RefreshBeanModel.withBeanName(beanName)));
        return result;
    }

    private void refresh(RefreshBeanEvent refreshBeanEvent) {
        applicationEventPublisher.publishEvent(refreshBeanEvent);
    }
}
