package com.risingoak.stash.plugins.hook;

import com.atlassian.stash.content.*;
import com.atlassian.stash.history.HistoryService;
import com.atlassian.stash.hook.repository.AsyncPostReceiveRepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.mail.MailMessage;
import com.atlassian.stash.mail.MailService;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryMetadataService;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.StashUser;
import com.atlassian.stash.util.Page;
import com.atlassian.stash.util.PageRequestImpl;
import com.atlassian.templaterenderer.TemplateRenderer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class EmailNotificationHook implements AsyncPostReceiveRepositoryHook {
    private final MailService mailService;
    private final StashAuthenticationContext stashAuthenticationContext;
    private final HistoryService historyService;
    private final ContentService contentService;
    private final RepositoryMetadataService repositoryMetadataService;
    private TemplateRenderer templateRenderer;


    public EmailNotificationHook(MailService mailService,
                                 StashAuthenticationContext stashAuthenticationContext,
                                 HistoryService historyService,
                                 ContentService contentService,
                                 RepositoryMetadataService repositoryMetadataService, TemplateRenderer templateRenderer) {
        this.mailService = mailService;
        this.stashAuthenticationContext = stashAuthenticationContext;
        this.historyService = historyService;
        this.contentService = contentService;
        this.repositoryMetadataService = repositoryMetadataService;
        this.templateRenderer = templateRenderer;
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
                if (RefChangeType.ADD.equals(refChange.getType()) || RefChangeType.DELETE.equals(refChange.getType())) {
                    if (settings.notifyOnBranchCreateDelete()) {
                        sendBranchAddDeleteEmail(context.getRepository(), refChange, currentUser, settings);
                    }
                } else if (RefChangeType.UPDATE.equals(refChange.getType())) {
                    String defaultBranchRefId = repositoryMetadataService.getDefaultBranch(context.getRepository()).getId();
                    if (settings.notifyOnAllCommits() || (defaultBranchRefId.equals(refChange.getRefId()) && settings.notifyOnDefaultBranch())) {
                        sendUpdateEmail(context.getRepository(), refChange, currentUser, settings);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendUpdateEmail(Repository repository, RefChange refChange, StashUser currentUser, EmailNotificationSettings settings) throws IOException {
        Page<Changeset> changesetPage = historyService.getChangesetsBetween(repository, refChange.getFromHash(), refChange.getToHash(), new PageRequestImpl(0, 100));
        String subject = getUpdateEmailSubject(repository, refChange, changesetPage, currentUser, settings);
        String body = getUpdateBody(repository, refChange, changesetPage, currentUser, settings);

        sendEmail(settings, currentUser, subject, body);
    }

    private String getUpdateEmailSubject(Repository repository, RefChange refChange, Page<Changeset> changesetPage, StashUser currentUser, EmailNotificationSettings settings) throws IOException {
        Map<String, Object> context = getCommonContext(repository, refChange, currentUser, settings);
        boolean forcePush = isForcePush(repository, refChange);
        context.put("isForcePush", forcePush);
        if (!forcePush) {
            context.put("numberOfCommits", getNumberOfCommits(repository, refChange, changesetPage));
        }
        StringWriter subject = new StringWriter();
        templateRenderer.render("/templates/email/update-subject.vm", context, subject);
        return subject.toString();
    }

    private String getUpdateBody(Repository repository, RefChange refChange, Page<Changeset> changesetPage, StashUser currentUser, EmailNotificationSettings settings) throws IOException {
        Map<String, Object> context = getCommonContext(repository, refChange, currentUser, settings);
        context.put("changesets", changesetPage.getValues());
        StringWriter body = new StringWriter();

        final List<String> filenames = new ArrayList<String>();
        if (settings.sendChangedFiled() && !settings.sendFullDiffs()) {
            contentService.streamDiff(repository, refChange.getFromHash(), refChange.getToHash(), "", new ChangedFilesDiffContentCallback(filenames));

        }
        if (settings.sendFullDiffs()) {
            StringBuffer buffer = new StringBuffer();
            renderFullDiffs(repository, refChange, buffer, filenames);
            context.put("diffContent", buffer.toString());
        }
        context.put("filenames", filenames);
        templateRenderer.render("/templates/email/update-body.vm", context, body);
        return body.toString();
    }

    private void sendBranchAddDeleteEmail(Repository repository, RefChange refChange, StashUser currentUser, EmailNotificationSettings settings) throws IOException {
        String subject = getAddDeleteEmailSubject(repository, refChange, currentUser, settings);
        String body = getBranchAddDeleteBody(repository, refChange, currentUser, settings);
        sendEmail(settings, currentUser, subject, body);
    }

    private String getAddDeleteEmailSubject(Repository repository, RefChange refChange, StashUser currentUser, EmailNotificationSettings settings) throws IOException {
        Map<String, Object> context = getCommonContext(repository, refChange, currentUser, settings);
        StringWriter subject = new StringWriter();
        templateRenderer.render("/templates/email/add-delete-subject.vm", context, subject);
        return subject.toString();
    }

    private String getBranchAddDeleteBody(Repository repository, RefChange refChange, StashUser currentUser, EmailNotificationSettings settings) throws IOException {
        Map<String, Object> context = getCommonContext(repository, refChange, currentUser, settings);
        StringWriter body = new StringWriter();
        templateRenderer.render("/templates/email/add-delete-body.vm", context, body);
        return body.toString();
    }

    private Map<String, Object> getCommonContext(Repository repository, RefChange refChange, StashUser currentUser, EmailNotificationSettings settings) {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("repository", repository);
        context.put("user", currentUser);
        context.put("refChange", refChange);
        context.put("settings", settings);
        return context;
    }

    private void renderFullDiffs(Repository repository, RefChange refChange, final StringBuffer buffer, final List<String> paths) {
        contentService.streamDiff(repository, refChange.getFromHash(), refChange.getToHash(), "", new FullDiffContentCallback(paths, buffer));
    }


    private String getNumberOfCommits(Repository repository, RefChange refChange, Page<Changeset> changesetPage) {
        if (!changesetPage.getIsLastPage()) {
            return ">" + changesetPage.getSize();
        } else {
            return String.valueOf(changesetPage.getSize());
        }
    }

    private boolean isForcePush(Repository repository, RefChange refChange) {
        return refChange.getType() == RefChangeType.UPDATE && historyService.getChangesetsBetween(
                repository,
                refChange.getToHash(),
                refChange.getFromHash(),
                new PageRequestImpl(0, 1)
        ).getSize() > 0;
    }

    private void sendEmail(EmailNotificationSettings settings, StashUser currentUser, String subject, String body) {
        MailMessage.Builder builder = new MailMessage.Builder();
        if (settings.sendFromAuthor() && isNotBlank(currentUser.getEmailAddress())) {
            builder.from(currentUser.getEmailAddress());
        }
        builder.to(settings.getEmails());
        builder.subject(subject);
        builder.text(body);
        if (settings.sendAsHtml()) {
            builder.header("Content-type", "text/html; charset=UTF-8");
        }
        mailService.submit(builder.build());
    }


}