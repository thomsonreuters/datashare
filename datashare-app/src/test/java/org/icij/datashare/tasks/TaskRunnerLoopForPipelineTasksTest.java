package org.icij.datashare.tasks;

import org.icij.datashare.PropertiesProvider;
import org.icij.datashare.extension.PipelineRegistry;
import org.icij.datashare.extract.DocumentCollectionFactory;
import org.icij.datashare.text.indexing.Indexer;
import org.icij.datashare.text.indexing.elasticsearch.ElasticsearchSpewer;
import org.icij.datashare.user.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class TaskRunnerLoopForPipelineTasksTest {
    @Mock TaskFactory taskFactory;
    @Mock BiFunction<String, Double, Void> updateCallback;
    @Mock ElasticsearchSpewer spewer;
    private final BlockingQueue<TaskView<?>> taskQueue = new LinkedBlockingQueue<>();
    private final TaskManagerMemory taskSupplier = new TaskManagerMemory(taskQueue, taskFactory);

    @Test
    public void test_scan_task() throws InterruptedException {
        TaskView<Long> task = new TaskView<>(ScanTask.class.getName(), User.local(), Map.of("dataDir", "/path/to/files"));
        ScanTask taskRunner = new ScanTask(mock(DocumentCollectionFactory.class), task, updateCallback);
        when(taskFactory.createScanTask(any(), any())).thenReturn(taskRunner);

        testTaskWithTaskRunner(task);
    }

    @Test
    public void test_index_task() throws Exception {
        TaskView<Long> task = new TaskView<>(IndexTask.class.getName(), User.local(), new HashMap<>());
        IndexTask taskRunner = new IndexTask(spewer, mock(DocumentCollectionFactory.class), task, updateCallback);
        when(taskFactory.createIndexTask(any(), any())).thenReturn(taskRunner);

        testTaskWithTaskRunner(task);
    }

    @Test
    public void test_extract_nlp_task() throws Exception {
        TaskView<Long> task = new TaskView<>(ExtractNlpTask.class.getName(), User.local(), Map.of("nlpPipeline", "EMAIL"));
        ExtractNlpTask taskRunner = new ExtractNlpTask(mock(Indexer.class), new PipelineRegistry(new PropertiesProvider()), mock(DocumentCollectionFactory.class), task, updateCallback);
        when(taskFactory.createExtractNlpTask(any(), any())).thenReturn(taskRunner);

        testTaskWithTaskRunner(task);
    }

    @Test
    public void test_scan_index_task() throws Exception {
        TaskView<Long> task = new TaskView<>(ScanIndexTask.class.getName(), User.local(), new HashMap<>());
        ScanIndexTask taskRunner = new ScanIndexTask(mock(DocumentCollectionFactory.class), mock(Indexer.class), task, updateCallback);
        when(taskFactory.createScanIndexTask(any(), any())).thenReturn(taskRunner);

        testTaskWithTaskRunner(task);
    }

    @Test
    public void test_enqueue_from_index_task() throws Exception {
        TaskView<Long> task = new TaskView<>(EnqueueFromIndexTask.class.getName(), User.local(), Map.of("nlpPipeline", "EMAIL"));
        EnqueueFromIndexTask taskRunner = new EnqueueFromIndexTask(mock(DocumentCollectionFactory.class), mock(Indexer.class), task, updateCallback);
        when(taskFactory.createEnqueueFromIndexTask(any(), any())).thenReturn(taskRunner);

        testTaskWithTaskRunner(task);
    }

    @Test
    public void test_deduplicate_task() throws Exception {
        TaskView<Long> task = new TaskView<>(DeduplicateTask.class.getName(), User.local(), new HashMap<>());
        DeduplicateTask taskRunner = new DeduplicateTask(mock(DocumentCollectionFactory.class), task, updateCallback);
        when(taskFactory.createDeduplicateTask(any(), any())).thenReturn(taskRunner);

        testTaskWithTaskRunner(task);
    }

    private void testTaskWithTaskRunner(TaskView<Long> task) throws InterruptedException {
        taskSupplier.startTask(task.name, User.local(), task.properties);
        taskSupplier.shutdownAndAwaitTermination(1, TimeUnit.SECONDS);

        assertThat(taskSupplier.getTasks()).hasSize(1);
        assertThat(taskSupplier.getTasks().get(0).name).isEqualTo(task.name);
    }

    @Before
    public void setUp() {
        initMocks(this);
        when(spewer.configure(any())).thenReturn(spewer);
    }
}
