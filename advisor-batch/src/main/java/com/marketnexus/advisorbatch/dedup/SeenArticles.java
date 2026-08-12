package com.marketnexus.advisorbatch.dedup;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class SeenArticles {

    private final Set<String> seenIds = Collections.synchronizedSet(new HashSet<>());

    public boolean isNew(String id) {
        return seenIds.add(id);
    }
}
