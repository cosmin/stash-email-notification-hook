package com.risingoak.stash.plugins.hook;

import com.atlassian.stash.content.Changeset;
import com.atlassian.stash.content.ContentService;
import com.atlassian.stash.hook.repository.AsyncPostReceiveRepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.mail.MailMessage;
import com.atlassian.stash.mail.MailService;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.repository.*;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.StashUser;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.risingoak.stash.common.api.git.PushService;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class EmailNotificationHook implements AsyncPostReceiveRepositoryHook {
    private final MailService mailService;
    private final StashAuthenticationContext stashAuthenticationContext;
    private final ContentService contentService;
    private final RepositoryMetadataService repositoryMetadataService;
    private final TemplateRenderer templateRenderer;
    private final NavBuilder navBuilder;
    private final PushService pushService;


    public EmailNotificationHook(MailService mailService,
                                 StashAuthenticationContext stashAuthenticationContext,
                                 ContentService contentService,
                                 RepositoryMetadataService repositoryMetadataService, TemplateRenderer templateRenderer, NavBuilder navBuilder, PushService pushService) {
        this.mailService = mailService;
        this.stashAuthenticationContext = stashAuthenticationContext;
        this.contentService = contentService;
        this.repositoryMetadataService = repositoryMetadataService;
        this.templateRenderer = templateRenderer;
        this.navBuilder = navBuilder;
        this.pushService = pushService;
    }

    @Override
    public void postReceive(@Nonnull RepositoryHookContext context, @Nonnull Collection<RefChange> refChanges) {
        if (!mailService.isHostConfigured()) {
            return;
        }
        EmailNotificationSettings settings = new EmailNotificationSettings(context.getSettings());
        if (!settings.hasValidEmails()) {
            return;
        }

        StashUser currentUser = stashAuthenticationContext.getCurrentUser();

        for (RefChange refChange : refChanges) {
            try {
                String defaultBranchRefId = repositoryMetadataService.getDefaultBranch(context.getRepository()).getId();
                if (settings.notifyOnAllCommits() || defaultBranchRefId.equals(refChange.getRefId())) {
                    prepareNotificationEmail(context.getRepository(), refChange, currentUser, settings);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void prepareNotificationEmail(Repository repository, RefChange refChange, StashUser currentUser, EmailNotificationSettings settings) throws IOException {
        final Map<String, Object> context = getCommonContext(repository, refChange, currentUser, settings);
        List<Changeset> commits = pushService.getChangesetsIntroducedBy(repository, refChange);
        List<Changeset> removedCommits = pushService.getChangesetsRemovedBy(repository, refChange);
        context.put("commits", commits);
        context.put("removedCommits", removedCommits);
        context.put("isForcePush", pushService.isForcePush(repository, refChange));

        String subject = getUpdateEmailSubject(context);
        String body = getUpdateBody(context, repository, refChange, commits, removedCommits, settings);

        sendEmail(settings, subject, body);
    }

    private String getUpdateEmailSubject(Map<String, Object> context) throws IOException {
        StringWriter subject = new StringWriter();
        templateRenderer.render("/templates/email/email-subject.vm", context, subject);
        return subject.toString();
    }

    private String getUpdateBody(Map<String, Object> context, Repository repository, RefChange refChange, List<Changeset> commits, List<Changeset> removedCommits, EmailNotificationSettings settings) throws IOException {
        StringWriter body = new StringWriter();

        final List<String> filenames = new ArrayList<String>();

        String fromHash;
        String toHash;

        if (RefChangeType.UPDATE == refChange.getType()) {
            fromHash = refChange.getFromHash();
            toHash = refChange.getToHash();
        } else if (RefChangeType.ADD == refChange.getType()) {
            toHash = refChange.getToHash();
            if (!commits.isEmpty()) {
                fromHash = commits.get(commits.size() - 1).getParents().iterator().next().getId();
            } else {
                fromHash = toHash;
            }

        } else { //(RefChangeType.DELETE == refChange.getType()) {
            toHash = refChange.getFromHash();
            if (!removedCommits.isEmpty()) {
                fromHash = removedCommits.get(removedCommits.size() - 1).getParents().iterator().next().getId();
            } else {
                fromHash = toHash;
            }
        }

        if (settings.sendChangedFiled() && !settings.sendFullDiffs()) {
            contentService.streamDiff(repository, fromHash, toHash, "", new ChangedFilesDiffContentCallback(filenames));

        }

        if (settings.sendFullDiffs()) {
            StringBuffer buffer = new StringBuffer();
            renderFullDiffs(repository, fromHash, toHash, buffer, filenames);
            context.put("diffContentWithHtml", buffer.toString());
        }
        context.put("filenames", filenames);
        templateRenderer.render("/templates/email/email-body.vm", context, body);
        return body.toString();
    }

    private Map<String, Object> getCommonContext(Repository repository, RefChange refChange, StashUser currentUser, EmailNotificationSettings settings) {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("repository", repository);
        context.put("user", currentUser);
        context.put("settings", settings);
        context.put("navBuilder", navBuilder);
        context.put("refChange", refChange);
        String refType = "";
        Ref ref;

        if (refChange.getRefId().contains("refs/heads")) {
            refType = "branch ";
        } else if (refChange.getRefId().contains("refs/tags")) {
            refType = "tag ";
        }

        if (RefChangeType.DELETE == refChange.getType()) {
            ref = new DeletedRef(refChange.getRefId());
        } else {
            ref = repositoryMetadataService.resolveRef(repository, refChange.getRefId());
        }

        context.put("ref", ref);
        context.put("refType", refType);

        for(RefChangeType refChangeType : RefChangeType.values()) {
            context.put(refChangeType.name(), refChangeType);
        }
        return context;
    }

    private void renderFullDiffs(Repository repository, String fromHash, String toHash, final StringBuffer buffer, final List<String> paths) {
        contentService.streamDiff(repository, fromHash, toHash, "", new FullDiffContentCallback(paths, buffer));
    }

    private void sendEmail(EmailNotificationSettings settings, String subject, String body) {
        MailMessage.Builder builder = new MailMessage.Builder();
        subject = subject.trim();
        body = body.trim();
        builder.to(settings.getEmails());
        builder.subject(subject);
        builder.text(body);
        if (settings.sendAsHtml()) {
            builder.header("Content-type", "text/html; charset=UTF-8");
        }
        mailService.submit(builder.build());
    }


}
