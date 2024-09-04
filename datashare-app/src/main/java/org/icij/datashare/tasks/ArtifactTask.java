package org.icij.datashare.tasks;

import com.google.inject.assistedinject.Assisted;
import org.icij.datashare.PropertiesProvider;
import org.icij.datashare.Stage;
import org.icij.datashare.asynctasks.Task;
import org.icij.datashare.extract.DocumentCollectionFactory;
import org.icij.datashare.function.Pair;
import org.icij.datashare.text.Document;
import org.icij.datashare.text.Project;
import org.icij.datashare.text.indexing.Indexer;
import org.icij.datashare.text.indexing.elasticsearch.SourceExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static org.icij.datashare.cli.DatashareCliOptions.ARTIFACT_DIR_OPT;
import static org.icij.datashare.cli.DatashareCliOptions.DEFAULT_DEFAULT_PROJECT;
import static org.icij.datashare.cli.DatashareCliOptions.DEFAULT_PROJECT_OPT;

public class ArtifactTask extends PipelineTask<Pair<String, String>> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Indexer indexer;
    private final Project project;
    private final Path artifactDir;
    static Pair<String, String> POISON = new Pair<>("POISON", "POISON");

    public ArtifactTask(DocumentCollectionFactory<Pair<String, String>> factory, Indexer indexer, PropertiesProvider propertiesProvider, @Assisted Task<Long> taskView, @Assisted final Function<Double, Void> updateCallback) {
        super(Stage.ARTIFACT, taskView.getUser(), factory, propertiesProvider, (Class<Pair<String, String>>)(Object)Pair.class);
        this.indexer = indexer;
        project = Project.project(ofNullable((String)taskView.args.get(DEFAULT_PROJECT_OPT)).orElse(DEFAULT_DEFAULT_PROJECT));
        artifactDir = Path.of(ofNullable((String)taskView.args.get(ARTIFACT_DIR_OPT)).orElseThrow(() -> new IllegalArgumentException(String.format("cannot create artifact task with empty %s", ARTIFACT_DIR_OPT))));
    }

    @Override
    public Long call() throws Exception {
        super.call();
        logger.info("creating artifact cache in {} for project {} from queue {}", artifactDir, project, inputQueue.getName());
        SourceExtractor extractor = new SourceExtractor(artifactDir.resolve(project.name));
        List<String> sourceExcludes = List.of("content", "content_translated");
        Pair<String, String> docRef;
        long nbDocs = 0;
        while (!(POISON.equals(docRef = inputQueue.poll(60, TimeUnit.SECONDS)))) {
            try {
                if (docRef != null) {
                    Document doc = indexer.get(project.name, docRef._1(), docRef._2(), sourceExcludes);
                    // we are getting a file input stream that is only created if we call the Supplier<InputStream>.get()
                    // so it is safe to ignore the return value, it will just create the file
                    extractor.getEmbeddedSource(project, doc);
                    nbDocs++;
                }
            } catch (Throwable e) {
                logger.error("error in ArtifactTask loop", e);
            }
        }
        logger.info("exiting ArtifactTask loop after {} document(s).", nbDocs);
        return nbDocs;
    }
}
