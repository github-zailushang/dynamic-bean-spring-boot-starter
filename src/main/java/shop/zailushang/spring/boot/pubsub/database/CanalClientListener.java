package shop.zailushang.spring.boot.pubsub.database;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import shop.zailushang.spring.boot.model.RefreshBeanModel;
import shop.zailushang.spring.boot.pubsub.event.RefreshBeanEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class CanalClientListener {

    private final CanalConnector canalConnector;

    private final ApplicationEventPublisher applicationEventPublisher;

    private static final Map<String, Integer> constructorMap = Map.of(
            "id", 0,
            "bean_name", 1,
            "lambda_script", 2,
            "description", 3
    );
    @SuppressWarnings("InfiniteLoopStatement")
    public void startListener() {
        while (true) {
            try {
                var message = canalConnector.getWithoutAck(1000);
                var batchId = message.getId();
                var size = message.getEntries().size();
                if (batchId == -1 || size == 0) {
                    TimeUnit.SECONDS.sleep(1);
                    continue;
                }
                processMessage(message);
                canalConnector.ack(batchId);
            } catch (Exception e) {
                log.error("canal client error: ", e);
                canalConnector.rollback();
            }
        }
    }

    private void processMessage(Message message) {
        message.getEntries()
                .stream()
                .filter(entry -> entry.getEntryType() == CanalEntry.EntryType.ROWDATA)
                .forEach(this::processEntry);
    }

    private void processEntry(CanalEntry.Entry entry) {
        try {
            var rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            var eventType = rowChange.getEventType();
            rowChange.getRowDatasList()
                    .forEach(rowData -> processRowData(eventType, rowData));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    private void processRowData(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        switch (eventType) {
            case INSERT -> handleInsert(rowData.getAfterColumnsList());
            case UPDATE -> handleUpdate(rowData.getBeforeColumnsList(), rowData.getAfterColumnsList());
            case DELETE -> handleDelete(rowData.getBeforeColumnsList());
            default -> {// 忽略其他事件
            }
        }
    }

    private RefreshBeanModel mapToModel(List<CanalEntry.Column> columns) {
        var array = new String[4];
        columns.forEach(column -> {
            var name = column.getName();
            var value = column.getValue();
            var index = constructorMap.get(name);
            array[index] = value;
        });
        return new RefreshBeanModel(Long.valueOf(array[0]), array[1], array[2], array[3]);
    }

    // 处理新增数据
    private void handleInsert(List<CanalEntry.Column> columns) {
        var refreshBeanModel = mapToModel(columns);
        log.info("Insert:{}", refreshBeanModel);
        applicationEventPublisher.publishEvent(RefreshBeanEvent.addWith(refreshBeanModel));
    }

    // 处理更新数据
    private void handleUpdate(List<CanalEntry.Column> before, List<CanalEntry.Column> after) {
        var beforeModel = mapToModel(before);
        var afterModel = mapToModel(after);
        log.info("Update - before:{} after:{}", beforeModel, afterModel);
        applicationEventPublisher.publishEvent(RefreshBeanEvent.updateWith(beforeModel, afterModel));
    }

    // 处理删除数据
    private void handleDelete(List<CanalEntry.Column> columns) {
        var refreshBeanModel = mapToModel(columns);
        log.info("Delete:{}", refreshBeanModel);
        applicationEventPublisher.publishEvent(RefreshBeanEvent.deleteWith(refreshBeanModel));
    }
}
