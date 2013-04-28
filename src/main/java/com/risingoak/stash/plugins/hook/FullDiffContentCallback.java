package com.risingoak.stash.plugins.hook;

import com.atlassian.stash.comment.DiffCommentAnchor;
import com.atlassian.stash.content.ConflictMarker;
import com.atlassian.stash.content.DiffContentCallback;
import com.atlassian.stash.content.DiffSegmentType;
import com.atlassian.stash.content.Path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

class FullDiffContentCallback implements DiffContentCallback {
    private final List<String> paths;
    private final StringBuffer buffer;
    public DiffSegmentType currentSegmentType;

    public FullDiffContentCallback(List<String> paths, StringBuffer buffer) {
        this.paths = paths;
        this.buffer = buffer;
        currentSegmentType = null;
    }

    @Override
    public void onDiffStart(@Nullable Path src, @Nullable Path dst) throws IOException {
        String pathToUse = (src == null ? dst.toString() : src.toString());
        paths.add(pathToUse);
        buffer.append("<a id=\"").append(pathToUse.replace("/", "").replace(" ", "")).append("\"></a>\n");

        if (src == null) {
            buffer.append("<div style=\"border: 1px solid #ccc;margin: 10px 0;\">\n");
            buffer.append("<h4 style=\"font-family: verdana, arial, helvetica, sans-serif;font-size: 10pt;padding: 8px;background: rgb(49, 104, 49);color: #fff;margin: 0;\">Added: ").append(escapeHtml(dst.toString())).append("</h4>\n");
        } else if (dst == null) {
            buffer.append("<div style=\"border: 1px solid #ccc;margin: 10px 0;\">\n");
            buffer.append("<h4 style=\"font-family: verdana, arial, helvetica, sans-serif;font-size: 10pt;padding: 8px;background: rgb(129, 32, 13);color: #fff;margin: 0;\">");
            buffer.append("Deleted: ").append(escapeHtml(src.toString())).append("</h4>\n");
        } else if (!src.equals(dst)) {
            buffer.append("<div style=\"border: 1px solid #ccc;margin: 10px 0;\">\n");
            buffer.append("<h4 style=\"font-family: verdana, arial, helvetica, sans-serif;font-size: 10pt;padding: 8px;background: #369;color: #fff;margin: 0;\">");
            buffer.append("Renamed: ");
            buffer.append(escapeHtml(src.toString()));
            buffer.append(" => ");
            buffer.append(escapeHtml(dst.toString()));
            buffer.append("</h4>\n");
        } else {
            buffer.append("<div style=\"border: 1px solid #ccc;margin: 10px 0;\">\n");
            buffer.append("<h4 style=\"font-family: verdana, arial, helvetica, sans-serif;font-size: 10pt;padding: 8px;background: #369;color: #fff;margin: 0;\">");
            buffer.append("Modified: ").append(escapeHtml(dst.toString())).append("</h4>\n");
        }
        buffer.append("<pre style=\"line-height: 1.2em;margin: 0;width: 100%;background: #eee;overflow: auto;\">");
        buffer.append("<span style=\"display: block;\"><span style=\"color: #888;background: #fff;display: block;padding: 0 10px 10px 10px;\">\n");
        buffer.append("--- ").append(src != null ? escapeHtml(src.toString()) : "/dev/null").append("\n");
        buffer.append("+++ ").append(dst != null ? escapeHtml(dst.toString()) : "/dev/null").append("\n");
    }

    @Override
    public void offerAnchors(@Nonnull List<? extends DiffCommentAnchor> diffCommentAnchors) {
    }

    @Override
    public void onBinary(@Nullable Path src, @Nullable Path dst) throws IOException {
        String pathToUse = (src == null ? dst.toString() : src.toString());
        paths.add(pathToUse);
        buffer.append("<a id=\"").append(pathToUse).append("\"></a>\n");

        if (src == null) {
            buffer.append("<div style=\"border: 1px solid #ccc;margin: 10px 0;\">\n");
            buffer.append("<h4 style=\"font-family: verdana, arial, helvetica, sans-serif;font-size: 10pt;padding: 8px;background: rgb(49, 104, 49);color: #fff;margin: 0;\">");
            buffer.append("Added (binary): ").append(escapeHtml(dst.toString())).append("</h4>");
        } else if (dst == null) {
            buffer.append("<div style=\"border: 1px solid #ccc;margin: 10px 0;\">\n");
            buffer.append("<h4 style=\"font-family: verdana, arial, helvetica, sans-serif;font-size: 10pt;padding: 8px;background: rgb(129, 32, 13);color: #fff;margin: 0;\">");
            buffer.append("Deleted (binary): ").append(escapeHtml(src.toString())).append("</h4>");
        } else if (!src.equals(dst)) {
            buffer.append("<div style=\"border: 1px solid #ccc;margin: 10px 0;\">\n");
            buffer.append("<h4 style=\"font-family: verdana, arial, helvetica, sans-serif;font-size: 10pt;padding: 8px;background: #369;color: #fff;margin: 0;\">");
            buffer.append("Renamed (binary): ");
            buffer.append(escapeHtml(src.toString()));
            buffer.append(" => ");
            buffer.append(escapeHtml(dst.toString()));
        } else {
            buffer.append("<div style=\"border: 1px solid #ccc;margin: 10px 0;\">\n");
            buffer.append("<h4 style=\"font-family: verdana, arial, helvetica, sans-serif;font-size: 10pt;padding: 8px;background: #369;color: #fff;margin: 0;\">");
            buffer.append("Modified (binary): ").append(escapeHtml(dst.toString())).append("</h4>");
        }
        buffer.append("</div>");
    }

    @Override
    public void onDiffEnd(boolean truncated) throws IOException {
        if (truncated) {
            // TODO handle truncated diffs
            // buffer.append("<span>... diff truncated ...</span>");
        }
        buffer.append("</span></pre></div>");
    }


    @Override
    public void onHunkStart(int srcLine, int srcSpan, int dstLine, int dstSpan) throws IOException {
        buffer.append("<span style=\"display: block;color: #888;background: #fff;\">@@ ");
        buffer.append("-").append(srcLine).append(",").append(srcSpan).append(" +").append(dstLine).append(",").append(dstSpan);
        buffer.append("@@</span>\n");
    }

    @Override
    public void onHunkEnd(boolean truncated) throws IOException {
        if (truncated) {
            // TODO handle truncated hunks
            // buffer.append("<span>... hunk truncated ...</span>");
        }
    }

    @Override
    public void onSegmentStart(@Nonnull DiffSegmentType diffSegmentType) throws IOException {
        this.currentSegmentType = diffSegmentType;
    }

    @Override
    public void onSegmentLine(@Nonnull String line, @Nullable ConflictMarker marker, boolean truncated) throws IOException {
        if (currentSegmentType == DiffSegmentType.CONTEXT) {
            buffer.append("<span style=\"display: block;\">&nbsp;");
            buffer.append(escapeHtml(line));
            buffer.append("</span>\n");
        } else if (currentSegmentType == DiffSegmentType.ADDED) {
            buffer.append("<ins style=\"background: #dfd;text-decoration: none;display: block;\">+");
            buffer.append(escapeHtml(line));
            buffer.append("</ins>");
        } else if (currentSegmentType == DiffSegmentType.REMOVED) {
            buffer.append("<del style=\"background: #fdd;text-decoration: none;display: block;\">-");
            buffer.append(escapeHtml(line));
            buffer.append("</del>");
        }
    }

    @Override
    public void onSegmentEnd(boolean truncated) throws IOException {
        currentSegmentType = null;
    }
}
