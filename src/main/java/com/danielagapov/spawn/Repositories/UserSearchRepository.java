package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import java.util.List;

public class UserSearchRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public List<User> fuzzySearchByName(String query) {
        SearchSession searchSession = Search.session(entityManager);

        SearchResult<User> result = searchSession.search(User.class)
                .where(f -> f.match()
                        .fields("firstName", "lastName", "username")
                        .matching(query)
                        .fuzzy(2) // Allow up to 2 edits (like Jaro-Winkler)
                )
                .fetch(10); // Get top 10 results

        return result.hits();
    }
}
