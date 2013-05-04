package com.risingoak.stash.plugins.hook;

import com.atlassian.stash.repository.Ref;

import javax.annotation.Nonnull;

public class DeletedRef implements Ref {
    private final String id;

    public DeletedRef(String id) {
        this.id = id;
    }

    @Nonnull
    @Override
    public String getDisplayId() {
        String[] components = id.split("/");
        return components[components.length - 1];
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    @Override
    public String getLatestChangeset() {
        return null;
    }
}
