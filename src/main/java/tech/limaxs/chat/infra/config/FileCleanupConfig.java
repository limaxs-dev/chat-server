package tech.limaxs.chat.infra.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FileCleanupConfig {

    @ConfigProperty(name = "file.cleanup.confirmation-timeout-hours", defaultValue = "24")
    private int confirmationTimeoutHours;

    @ConfigProperty(name = "file.cleanup.retention-days", defaultValue = "30")
    private int retentionDays;

    @ConfigProperty(name = "file.cleanup.enabled", defaultValue = "true")
    private boolean enabled;

    @ConfigProperty(name = "file.cleanup.unconfirmed-cron", defaultValue = "0 0 * * * ?")
    private String unconfirmedCron;

    @ConfigProperty(name = "file.cleanup.expired-cron", defaultValue = "0 0 3 * * ?")
    private String expiredCron;

    public int getConfirmationTimeoutHours() {
        return confirmationTimeoutHours;
    }

    public void setConfirmationTimeoutHours(int confirmationTimeoutHours) {
        this.confirmationTimeoutHours = confirmationTimeoutHours;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUnconfirmedCron() {
        return unconfirmedCron;
    }

    public void setUnconfirmedCron(String unconfirmedCron) {
        this.unconfirmedCron = unconfirmedCron;
    }

    public String getExpiredCron() {
        return expiredCron;
    }

    public void setExpiredCron(String expiredCron) {
        this.expiredCron = expiredCron;
    }
}
