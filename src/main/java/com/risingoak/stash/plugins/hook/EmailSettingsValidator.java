package com.risingoak.stash.plugins.hook;

import com.atlassian.stash.mail.MailService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.RepositorySettingsValidator;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsValidationErrors;

import javax.annotation.Nonnull;

import static org.apache.commons.lang.StringUtils.isBlank;

public class EmailSettingsValidator implements RepositorySettingsValidator {
    private MailService mailService;

    public EmailSettingsValidator(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public void validate(@Nonnull Settings rawSettings, @Nonnull SettingsValidationErrors settingsValidationErrors, @Nonnull Repository repository) {
        //To change body of implemented methods use File | Settings | File Templates.
        if (!mailService.isHostConfigured()) {
            settingsValidationErrors.addFormError("Mail host is not configured. Check global settings.");
        }
        EmailNotificationSettings settings = new EmailNotificationSettings(rawSettings);
        validateToAddresses(rawSettings, settings, settingsValidationErrors);
        validateNotifyOn(settings, settingsValidationErrors);
        validateEmailOptions(settings, settingsValidationErrors);
    }

    private void validateEmailOptions(EmailNotificationSettings settings, SettingsValidationErrors settingsValidationErrors) {
        // nothing to do
    }

    private void validateNotifyOn(EmailNotificationSettings settings, SettingsValidationErrors settingsValidationErrors) {
        // nothing to do
    }

    private void validateToAddresses(Settings rawSettings, EmailNotificationSettings settings, SettingsValidationErrors settingsValidationErrors) {
        if (isBlank(rawSettings.getString("toAddresses", ""))) {
            settingsValidationErrors.addFieldError("toAddresses", "At least one to address is required");
        } else if (!settings.hasValidEmails()) {
            settingsValidationErrors.addFieldError("toAddresses", "Invalid email address.");
        }
    }
}
