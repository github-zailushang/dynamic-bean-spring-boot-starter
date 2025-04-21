package shop.zailushang.spring.boot.service;

import shop.zailushang.spring.boot.model.RefreshBeanModel;

import java.util.List;

public interface RefreshBeanService {

    List<RefreshBeanModel> selectAll();

    int insert(RefreshBeanModel refreshBeanModel);

    int update(RefreshBeanModel refreshBeanModel);

    int delete(String beanName);
}