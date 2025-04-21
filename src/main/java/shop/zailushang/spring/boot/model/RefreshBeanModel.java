package shop.zailushang.spring.boot.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shop.zailushang.spring.boot.util.Assert;

public record RefreshBeanModel(Long id, String beanName, String lambdaScript, String description) {
    public static RefreshBeanModel withBeanName(String beanName) {
        return new RefreshBeanModel(null, beanName, null, null);
    }

    // 只有当修改了 lambdaScript 时，返回 true
    public boolean diff(RefreshBeanModel another) {
        Assert.isTrue(another, Assert::isNotNull, () -> new NullPointerException("another is null"));
        return Assert.isNotEq(lambdaScript, another.lambdaScript);
    }

    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static RefreshBeanModel parse(String jsonStr) {
        try {
            return new ObjectMapper().readValue(jsonStr, RefreshBeanModel.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}