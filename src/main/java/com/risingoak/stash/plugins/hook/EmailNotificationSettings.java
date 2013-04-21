package com.risingoak.stash.plugins.hook;

import com.atlassian.stash.setting.Settings;
import org.hibernate.validator.internal.constraintvalidators.EmailValidator;

import java.util.Arrays;

public class EmailNotificationSettings {
    private Settings settings;

    public EmailNotificationSettings(Settings settings) {
        this.settings = settings;
    }

    public boolean sendFromAuthor() {
        return settings.getBoolean("sendFromAuthor", false);
    }

    public boolean sendCommitMessages() {
        return settings.getBoolean("commitMessages", false);
    }

    public boolean sendChangedFiled() {
        return settings.getBoolean("changedFiles", false);
    }

    public boolean sendFullDiffs() {
        return settings.getBoolean("fullDiffs", false);
    }

    public boolean notifyOnDefaultBranch() {
        return settings.getBoolean("defaultBranch", false);
    }

    public boolean notifyOnBranchCreateDelete() {
        return settings.getBoolean("branchCreateDelete", false);
    }

    public boolean notifyOnAllCommits() {
        return settings.getBoolean("allBranches", false);
    }

    public Iterable<String> getEmails() {
        return Arrays.asList(settings.getString("toAddresses", "").split(","));
    }

    public boolean sendAsHtml() {
        return true;
    }

    public boolean hasValidEmails() {
        boolean hasOne = false;
        for (String email : getEmails()) {
            if (!isEmailValid(email)) {
                return false;
            } else {
                hasOne = true;
            }
        }
        return hasOne;
    }

    private boolean isEmailValid(String email) {
        return new EmailValidator().isValid(email, null);
    }
}