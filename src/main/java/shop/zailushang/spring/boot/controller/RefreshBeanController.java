package shop.zailushang.spring.boot.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import shop.zailushang.spring.boot.util.Assert;
import shop.zailushang.spring.boot.model.RefreshBeanModel;
import shop.zailushang.spring.boot.service.RefreshBeanService;


@RestController
@RequestMapping("/refreshBean")
@ConditionalOnExpression("'${dynamic-bean.mode}' == 'redis' || '${dynamic-bean.mode}' == 'database'")
public record RefreshBeanController(RefreshBeanService refreshBeanService, ApplicationContext applicationContext) {
    @PostMapping
    public Object createRefreshBean(@RequestBody RefreshBeanModel refreshBeanModel) {
        Assert.isTrue(refreshBeanModel.beanName(), Assert::strNotBlank, () -> new IllegalArgumentException("beanName can't be empty"));
        Assert.isTrue(refreshBeanModel.lambdaScript(), Assert::strNotBlank, () -> new IllegalArgumentException("lambdaScript can't be empty"));
        Assert.isTrue(refreshBeanModel.description(), Assert::strNotBlank, () -> new IllegalArgumentException("description can't be empty"));
        return refreshBeanService.insert(refreshBeanModel);
    }

    @DeleteMapping("/{beanName}")
    public Object removeRefreshBean(@PathVariable("beanName") String beanName) {
        return refreshBeanService.delete(beanName);
    }

    @PutMapping
    public Object updateRefreshBean(@RequestBody RefreshBeanModel refreshBeanModel) {
        Assert.isTrue(refreshBeanModel.beanName(), Assert::strNotBlank, () -> new IllegalArgumentException("beanName can't be empty"));
        Assert.isTrue(
                refreshBeanModel,
                rbm -> Assert.strNotBlank(rbm.lambdaScript()) || Assert.strNotBlank(rbm.description()),
                () -> new IllegalArgumentException("lambdaScript, description can't be empty together")
        );
        return refreshBeanService.update(refreshBeanModel);
    }

    @GetMapping("/list")
    public Object listRefreshBean() {
        return refreshBeanService.selectAll();
    }
}
