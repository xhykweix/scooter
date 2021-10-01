package tool;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper
{
    private static final String SHARED_PREF_NAME = "KEYSTORE_SETTING";
    private static final String PREF_KEY_AES = "PREF_KEY_AES";
    private static final String PREF_KEY_IV = "PREF_KEY_IV";
    private static final String PREF_KEY_INPUT = "PREF_KEY_INPUT";
    private SharedPreferences sharedPreferences;

    public SharedPreferencesHelper(Context context)//建構子
    {
        sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setData(String key,String value)
    {
        sharedPreferences.edit().putString(key, value).commit();
    }

    public void setData(String key,boolean value)
    {
        sharedPreferences.edit().putBoolean(key, value).commit();
    }

    public String getString(String key)
    {
        return sharedPreferences.getString(key, "");
    }

    private boolean getBoolean(String key)
    {
        return sharedPreferences.getBoolean(key, false);
    }

    public void setIV(String value)
    {
        setData(PREF_KEY_IV, value);
    }

    public String getIV()
    {
        return getString(PREF_KEY_IV);
    }

    public void setAESKey(String value)
    {
        setData(PREF_KEY_AES, value);
    }

    public String getAESKey()
    {
        return getString(PREF_KEY_AES);
    }

    public void setInput(String value)
    {
        setData(PREF_KEY_INPUT, value);
    }

    public String getInput()
    {
        return getString(PREF_KEY_INPUT);
    }
    public void clear()
    {
        String tempIV=getIV();
        String tempAESKey=getAESKey();
        sharedPreferences.edit().clear().putString(PREF_KEY_IV,tempIV).putString(PREF_KEY_AES,tempAESKey).commit();

    }
}