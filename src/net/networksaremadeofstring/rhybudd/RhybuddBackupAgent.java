package net.networksaremadeofstring.rhybudd;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class RhybuddBackupAgent extends BackupAgentHelper 
{
    // The name of the SharedPreferences file
    static final String PREFS = "user_preferences";

    // A key to uniquely identify the set of backup data
    static final String PREFS_BACKUP_KEY = "prefs";

    // Allocate a helper and add it to the backup agent
    @Override
    public void onCreate() 
    {
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this,"net.networksaremadeofstring.rhybudd_preferences");
        addHelper(PREFS_BACKUP_KEY, helper);
    }
}