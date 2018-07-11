package org.icij.datashare;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import net.codestory.http.filters.Filter;
import net.codestory.http.security.SessionIdStore;
import net.codestory.http.security.Users;
import org.elasticsearch.client.Client;
import org.icij.datashare.com.Publisher;
import org.icij.datashare.com.redis.RedisPublisher;
import org.icij.datashare.session.LocalUserFilter;
import org.icij.datashare.session.OAuth2CookieFilter;
import org.icij.datashare.session.RedisSessionIdStore;
import org.icij.datashare.session.RedisUsers;
import org.icij.datashare.text.indexing.Indexer;
import org.icij.datashare.text.indexing.LanguageGuesser;
import org.icij.datashare.text.indexing.elasticsearch.ElasticsearchIndexer;
import org.icij.datashare.text.indexing.elasticsearch.language.OptimaizeLanguageGuesser;

import java.util.Map;
import java.util.Properties;

import static java.lang.Boolean.parseBoolean;
import static org.icij.datashare.text.indexing.elasticsearch.ElasticsearchConfiguration.createESClient;
import static org.icij.datashare.text.indexing.elasticsearch.ElasticsearchConfiguration.createIndex;
import static org.icij.datashare.user.User.local;

public class ProdServiceModule extends AbstractModule{
    private final Properties properties;

    public ProdServiceModule(Properties properties) {
        this.properties = properties;
    }

    public ProdServiceModule(final Map<String, String> properties) {
        this.properties = new Properties();
        this.properties.putAll(properties);
    }

    @Override
    protected void configure() {
        PropertiesProvider propertiesProvider = properties == null ? new PropertiesProvider() : new PropertiesProvider().mergeWith(properties);
        bind(PropertiesProvider.class).toInstance(propertiesProvider);

        String authProp = propertiesProvider.get("auth").orElse("false");
        if (parseBoolean(authProp)) {
            bind(Users.class).to(RedisUsers.class);
            bind(SessionIdStore.class).to(RedisSessionIdStore.class);
            bind(Filter.class).to(OAuth2CookieFilter.class).asEagerSingleton();
        } else {
            bind(Filter.class).to(LocalUserFilter.class).asEagerSingleton();
        }
        Client esClient = createESClient(propertiesProvider);
        createIndex(esClient, local().indexName());
        bind(Client.class).toInstance(esClient);

        bind(LanguageGuesser.class).to(OptimaizeLanguageGuesser.class);
        bind(Publisher.class).to(RedisPublisher.class);
        bind(Indexer.class).to(ElasticsearchIndexer.class);
        install(new FactoryModuleBuilder().build(TaskFactory.class));
    }
}
