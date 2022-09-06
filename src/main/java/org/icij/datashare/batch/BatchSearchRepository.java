package org.icij.datashare.batch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.icij.datashare.text.Document;
import org.icij.datashare.user.User;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;

public interface BatchSearchRepository extends Closeable {
    boolean save(BatchSearch batchSearch);
    boolean saveResults(String batchSearchId, String query, List<Document> documents);
    boolean setState(String batchSearchId, BatchSearch.State state);
    boolean setState(String batchSearchId, SearchException error);
    boolean deleteAll(User user);
    boolean delete(User user, String batchId);


    List<BatchSearchRecord> getRecords(User user, List<String> projectsIds);
    int getTotal(User user, List<String> projectsIds, WebQuery webQuery);
    List<BatchSearchRecord> getRecords(User user, List<String> projectsIds, WebQuery webQuery);
    List<String> getQueued();
    List<SearchResult> getResults(User user, String batchSearchId);
    List<SearchResult> getResults(User user, String batchId, WebQuery webQuery);

    boolean publish(User user, String batchId, boolean published);

    BatchSearch get(String id);
    BatchSearch get(User user, String batchId);
    BatchSearch get(User user, String batchId, boolean withQueries);

    Map<String,Integer> getQueries(User user, String batchId, int from, int size, String search, String orderBy);

    boolean reset(String batchId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    class WebQuery {
        public static final String DEFAULT_SORT_FIELD = "doc_nb";
        public final String sort;
        public final String order;
        public final String query;
        public final String field;
        public final int from;
        public final int size;
        public final List<String> queries;
        public final List<String> project;
        public final List<String> batchDate;
        public final List<String> state;
        public final String publishState;
        public final boolean withQueries;

        @JsonCreator
        public WebQuery(@JsonProperty("size") int size, @JsonProperty("from") int from,
                        @JsonProperty("sort") String sort, @JsonProperty("order") String order,
                        @JsonProperty("query") String query, @JsonProperty("field") String field,
                        @JsonProperty("queries") List<String> queries, @JsonProperty("project") List<String> project,
                        @JsonProperty("batchDate") List<String> batchDate, @JsonProperty("state") List<String> state,
                        @JsonProperty("publishState") String publishState, @JsonProperty("withQueries") boolean withQueries) {
            this.size = size;
            this.from = from;
            this.sort = sort == null ? DEFAULT_SORT_FIELD : sort;
            this.query = query;
            this.field = field;
            this.order = sort == null ? "asc": order;
            this.queries = queries == null ? null: unmodifiableList(queries);
            this.project = project == null ? null: unmodifiableList(project);
            this.batchDate = batchDate == null ? null: unmodifiableList(batchDate);
            this.state = state == null ? null: unmodifiableList(state);
            this.publishState = publishState;
            this.withQueries = withQueries;
        }

        public WebQuery(int size, int from) { this(size, from, null, null, "*", "all", null, null, null, null, null, false);}
        public WebQuery() { this(0, 0, null, null, "*", "all", null, null, null, null, null, false);}

        // for tests
        public WebQuery(String sort, String order, String query, String field) {
            this(0, 0, sort, order, query, field, null,null,null,null,null, false);
        }

        public WebQuery(String query, String field) {
            this(null, null, query, field);
        }

        public WebQuery(List<String> queries) {
            this(0, 0, null, null,"*","all", queries,null,null,null, null, false);
        }

        public WebQuery(List<String> project, List<String> batchDate, List<String> state, String publishState) {
            this(0, 0, null, null,"*","all", null, project, batchDate, state, publishState, false);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WebQuery that = (WebQuery) o;
            return from == that.from &&
                    size == that.size &&
                    Objects.equals(sort, that.sort) &&
                    Objects.equals(order, that.order) &&
                    Objects.equals(query,that.query) &&
                    Objects.equals(field,that.field) &&
                    Objects.equals(queries, that.queries) &&
                    Objects.equals(project, that.project) &&
                    Objects.equals(batchDate, that.batchDate) &&
                    Objects.equals(state, that.state) &&
                    Objects.equals(publishState, that.publishState);
        }

        @Override
        public int hashCode() { return Objects.hash(sort, order, query, field, from, size, queries, project, batchDate, state, publishState); }
        public boolean hasFilteredQueries() { return queries !=null && !queries.isEmpty();}
        public boolean hasFilteredProjects() { return project !=null && !project.isEmpty();}
        public boolean hasFilteredDates() { return batchDate !=null && !batchDate.isEmpty();}
        public boolean hasFilteredStates() { return state !=null && !state.isEmpty();}
        public boolean hasFilteredPublishStates() { return publishState !=null && !publishState.isEmpty();}
        public boolean isSorted() { return !DEFAULT_SORT_FIELD.equals(this.sort);}
    }
}
