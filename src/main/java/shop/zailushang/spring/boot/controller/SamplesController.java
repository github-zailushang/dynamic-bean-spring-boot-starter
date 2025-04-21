package shop.zailushang.spring.boot.controller;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.zailushang.spring.boot.framework.SAM;

@RestController
@RequestMapping("/test")
public record SamplesController(ApplicationContext applicationContext) {
    @GetMapping("/{beanName}")
    public Object getRefreshBean(@PathVariable("beanName") String beanName) {
        var sam = applicationContext.getBean(beanName, SAM.class);
        sam.run();
        return "运行成功，请查看控制台。";
    }

    @GetMapping("/list")
    public Object setRefreshBean() {
        return applicationContext.getBeanNamesForType(SAM.class);
    }
}
