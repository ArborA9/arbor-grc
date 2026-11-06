package com.a9.arbor.grc.catalog.domain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves which published frameworks a tenant may activate under one licensed module.
 * Super Admin picks 1, some, or all; omitting ids still means the full latest-per-code pack
 * so existing API callers keep working. An empty list is never "all".
 */
public final class FrameworkEntitlements {

    private FrameworkEntitlements() {
    }

    /**
     * @param requested null/omit = latest published version per framework code;
     *                  otherwise only those ids (must be published under the module)
     * @throws IllegalArgumentException empty pick while published frameworks exist, or id not published
     */
    public static Set<Long> select(List<Long> requested, Collection<Long> publishedIds,
                                   Collection<Long> latestPerCode, String moduleCode) {
        Set<Long> published = publishedIds == null
                ? Set.of()
                : new LinkedHashSet<>(publishedIds);
        if (published.isEmpty()) {
            if (requested != null && !requested.isEmpty()) {
                throw new IllegalArgumentException(
                        "Framework " + requested.get(0) + " is not a published framework under " + moduleCode);
            }
            return Set.of();
        }
        Set<Long> selected;
        if (requested == null) {
            selected = latestPerCode == null
                    ? new LinkedHashSet<>()
                    : new LinkedHashSet<>(latestPerCode);
        } else {
            selected = new LinkedHashSet<>(requested);
            for (Long id : selected) {
                if (!published.contains(id)) {
                    throw new IllegalArgumentException(
                            "Framework " + id + " is not a published framework under " + moduleCode);
                }
            }
            if (selected.isEmpty()) {
                throw new IllegalArgumentException(
                        "Select at least one published framework under " + moduleCode);
            }
        }
        return selected;
    }
}
