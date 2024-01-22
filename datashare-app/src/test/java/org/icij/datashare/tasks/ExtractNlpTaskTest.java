package org.icij.datashare.tasks;

import org.icij.datashare.PropertiesProvider;
import org.icij.datashare.extract.MemoryDocumentCollectionFactory;
import org.icij.datashare.text.Document;
import org.icij.datashare.text.Language;
import org.icij.datashare.text.indexing.Indexer;
import org.icij.datashare.text.nlp.AbstractPipeline;
import org.icij.datashare.user.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;

import static java.util.Collections.emptyList;
import static org.icij.datashare.text.DocumentBuilder.createDoc;
import static org.icij.datashare.text.Language.ENGLISH;
import static org.icij.datashare.text.Project.project;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ExtractNlpTaskTest {
    @Mock private Indexer indexer;
    @Mock private AbstractPipeline pipeline;
    private final MemoryDocumentCollectionFactory<String> factory = new MemoryDocumentCollectionFactory<>();
    private ExtractNlpTask nlpTask;

    @Before
    public void setUp() {
        initMocks(this);
        nlpTask = new ExtractNlpTask(indexer, pipeline, factory, User.local(), new PropertiesProvider(new HashMap<>(){{
            put("maxContentLength", "32");
        }}).getProperties());
    }

    @Test
    public void test_on_message_does_nothing__when_doc_not_found_in_index() throws Exception {
        nlpTask.findNamedEntities(project("projectName"),"unknownId");

        verify(pipeline, never()).initialize(any(Language.class));
        verify(pipeline, never()).process(any());
    }

    @Test
    public void test_on_message_do_not_processNLP__when_init_fails() throws Exception {
        when(pipeline.initialize(any())).thenReturn(false);
        when(indexer.get(anyString(), anyString(), anyString())).thenReturn(createDoc("content").build());

        nlpTask.findNamedEntities(project("projectName"),"id");
        verify(pipeline, never()).process(any());
    }

    @Test
    public void test_on_message_processNLP__when_doc_found_in_index() throws Exception {
        when(pipeline.initialize(any())).thenReturn(true);
        Document doc = createDoc("content").build();
        when(pipeline.process(doc)).thenReturn(emptyList());
        when(indexer.get("projectName", doc.getId())).thenReturn(doc);

        nlpTask.findNamedEntities(project("projectName"), doc.getId());

        verify(pipeline).initialize(ENGLISH);
        verify(pipeline).process(doc);
    }

    @Test
    public void test_on_message_process__chunked_doc_when_doc_is_large()  throws Exception  {
        when(pipeline.initialize(any())).thenReturn(true);
        Document doc = createDoc("huge_doc").with("0123456789abcdef0123456789abcdef+").build();
        when(pipeline.process(doc)).thenReturn(emptyList());
        when(indexer.get("projectName", doc.getId())).thenReturn(doc);

        nlpTask.findNamedEntities(project("projectName"), doc.getId());

        verify(pipeline).initialize(ENGLISH);
        verify(pipeline).process(doc, 32, 0);
        verify(pipeline).process(doc, 32, 32);
    }
}
