package com.risingoak.stash.plugins.hook;

import com.atlassian.stash.content.AbstractDiffContentCallback;
import com.atlassian.stash.content.Path;

import java.io.IOException;
import java.util.List;

class ChangedFilesDiffContentCallback extends AbstractDiffContentCallback {
    private List<String> paths;

    public ChangedFilesDiffContentCallback(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public void onDiffStart(Path src, Path dst) throws IOException {
        if (dst == null) {
            paths.add(src.toString());
        } else {
            paths.add(dst.toString());
        }
    }
}
