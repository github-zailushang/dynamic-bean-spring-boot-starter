package shop.zailushang.spring.boot.pubsub.event;

public interface EventProcessor {
    void processEvent(RefreshBeanEvent refreshBeanEvent);
}
