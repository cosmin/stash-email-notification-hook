package com.risingoak.stash.plugins.hook;

import com.atlassian.stash.setting.Settings;
import org.hibernate.validator.internal.constraintvalidators.EmailValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmailNotificationSettings {
    private Settings settings;

    public EmailNotificationSettings(Settings settings) {
        this.settings = settings;
    }

    public boolean sendChangedFiled() {
        return settings.getBoolean("changedFiles", false);
    }

    public boolean sendFullDiffs() {
        return settings.getBoolean("fullDiffs", false);
    }

    public boolean notifyOnAllCommits() {
        return settings.getBoolean("allBranches", false);
    }

    public Iterable<String> getEmails() {
        List<String> emails = new ArrayList<String>();
        for(String address : Arrays.asList(settings.getString("toAddresses", "").split(","))) {
            emails.add(address.trim());
        }
        return emails;
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
