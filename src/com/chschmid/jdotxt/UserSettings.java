package com.chschmid.jdotxt;

import java.io.File;
import java.util.prefs.Preferences;

public class UserSettings {
    private final Preferences preferences;
    private static final String DEFAULT_DIR = System.getProperty("user.home") + File.separator + "jdotxt";

    public UserSettings(Preferences preferences) {
        this.preferences = preferences;
    }

    public boolean autosave(){
        return preferences.getBoolean("autosave", true);
    }

    public void autosave(boolean selected) {
        preferences.putBoolean("autosave", selected);

    }

    public String dataDir() {
        return preferences.get("dataDir", DEFAULT_DIR);
    }

    public void dataDir(String path) {
        preferences.put("dataDir",path);
    }
}
