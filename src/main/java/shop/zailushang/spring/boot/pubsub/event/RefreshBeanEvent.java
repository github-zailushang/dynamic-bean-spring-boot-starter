package shop.zailushang.spring.boot.pubsub.event;

import org.springframework.context.ApplicationEvent;
import shop.zailushang.spring.boot.model.RefreshBeanModel;

import java.util.LinkedHashMap;
import java.util.Map;

public class RefreshBeanEvent extends ApplicationEvent {

    public enum EventType {
        ADD,
        DEL
    }

    private RefreshBeanEvent(Map<EventType, RefreshBeanModel> refreshBeanModel) {
        super(refreshBeanModel);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<EventType, RefreshBeanModel> getSource() {
        return (Map<EventType, RefreshBeanModel>) super.getSource();
    }

    public static RefreshBeanEvent addWith(RefreshBeanModel refreshBeanModel) {
        return new RefreshBeanEvent(Map.of(EventType.ADD, refreshBeanModel));
    }

    public static RefreshBeanEvent deleteWith(RefreshBeanModel refreshBeanModel) {
        return new RefreshBeanEvent(Map.of(EventType.DEL, refreshBeanModel));
    }

    public static RefreshBeanEvent updateWith(RefreshBeanModel beforeModel, RefreshBeanModel afterModel) {
        var map = new LinkedHashMap<EventType, RefreshBeanModel>();
        map.put(EventType.DEL, beforeModel);
        map.put(EventType.ADD, afterModel);
        return new RefreshBeanEvent(map);
    }
}
