package org.icij.datashare.extension;

import org.icij.datashare.PropertiesProvider;
import org.icij.datashare.text.nlp.Pipeline;
import org.icij.datashare.text.nlp.test.TestPipeline;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import static org.fest.assertions.Assertions.assertThat;
import static org.icij.datashare.test.JarUtil.createJar;

public class PipelineRegistryTest {
    public @Rule TemporaryFolder folder = new TemporaryFolder();
    PipelineRegistry pipelineRegistry;

    @Test
    public void test_init_pipeline_registry_no_extension() throws FileNotFoundException {
        pipelineRegistry.load();
        assertThat(pipelineRegistry.getPipelineTypes()).isEmpty();
    }

    @Test
    public void test_load_pipeline_registry_one_extension() throws IOException {
        createJar(folder.getRoot().toPath(), "extension", new File("src/test/java/org/icij/datashare/text/nlp/test/TestPipeline.java"));
        pipelineRegistry.load();
        assertThat(pipelineRegistry.getPipelineTypes()).contains(Pipeline.Type.TEST);
    }

    @Test
    public void test_load_pipeline_registry_one_extension_with_interface() throws IOException {
        createJar(folder.getRoot().toPath(), "extension", new File("src/test/java/org/icij/datashare/text/nlp/test/TestPipeline.java"),
                new File("src/main/java/org/icij/datashare/text/nlp/AbstractPipeline.java"));
        pipelineRegistry.load();
        assertThat(pipelineRegistry.getPipelineTypes()).contains(Pipeline.Type.TEST);
    }

    @Test
    public void test_load_pipeline_registry_one_extension_with_unknown_class_from_classpath() throws IOException {
        createJar(folder.getRoot().toPath(), "extension", EXTENSION_PIPELINE_SOURCE);
        pipelineRegistry.load();
        assertThat(pipelineRegistry.getPipelineTypes()).contains(Pipeline.Type.TEST);
    }

    @Test
    public void test_register_pipeline_from_class() {
        pipelineRegistry.register(TestPipeline.class);
        assertThat(pipelineRegistry.getPipelineTypes()).contains(Pipeline.Type.TEST);
    }

    @Test
    public void test_register_pipeline_from_type() {
        pipelineRegistry.register(Pipeline.Type.TEST);
        assertThat(pipelineRegistry.getPipelineTypes()).contains(Pipeline.Type.TEST);
    }

    @Test(expected = FileNotFoundException.class)
    public void test_extension_dir_not_found() throws Exception {
        new PipelineRegistry(new PropertiesProvider(new HashMap<>())).load();
    }

    @Before
    public void setUp() {
        pipelineRegistry = new PipelineRegistry(new PropertiesProvider(new HashMap<String, String>() {{
            put(PropertiesProvider.EXTENSIONS_DIR, folder.getRoot().getPath());
        }}));
    }

    String EXTENSION_PIPELINE_SOURCE = "package org.icij.datashare.text.nlp.test;\n" +
            "\n" +
            "import org.icij.datashare.PropertiesProvider;\n" +
            "import org.icij.datashare.text.Document;\n" +
            "import org.icij.datashare.text.Language;\n" +
            "import org.icij.datashare.text.NamedEntity;\n" +
            "import org.icij.datashare.text.nlp.Annotations;\n" +
            "import org.icij.datashare.text.nlp.NlpStage;\n" +
            "import org.icij.datashare.text.nlp.Pipeline;\n" +
            "\n" +
            "import java.nio.charset.Charset;\n" +
            "import java.util.List;\n" +
            "import java.util.Optional;\n" +
            "\n" +
            "public class ExtensionPipeline implements Pipeline {\n" +
            "    @Override\n" +
            "    public Type getType() {\n" +
            "        return Type.TEST;\n" +
            "    }\n" +
            "\n" +
            "    public ExtensionPipeline(PropertiesProvider provider) {}\n" +
            "    @Override\n" +
            "    public boolean initialize(Language language) throws InterruptedException {\n" +
            "        return false;\n" +
            "    }\n" +
            "    @Override\n" +
            "    public List<NamedEntity> process(Document doc) throws InterruptedException  {\n" +
            "        return null;\n" +
            "    }" +
            "    @Override\n" +
            "    public void terminate(Language language) throws InterruptedException {\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public boolean supports(NlpStage stage, Language language) {\n" +
            "        return false;\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public List<NamedEntity.Category> getTargetEntities() {\n" +
            "        return null;\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public List<NlpStage> getStages() {\n" +
            "        return null;\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public boolean isCaching() {\n" +
            "        return false;\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public Charset getEncoding() {\n" +
            "        return null;\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public Optional<String> getPosTagSet(Language language) {\n" +
            "        return Optional.empty();\n" +
            "    }\n" +
            "}\n";
}
