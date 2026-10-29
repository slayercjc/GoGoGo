package com.zcshou.gogogo;

import android.os.Bundle;
import android.text.InputType;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.elvishew.xlog.XLog;
import com.zcshou.utils.GoUtils;

import java.util.Objects;

public class FragmentSettings extends PreferenceFragmentCompat {

    // Set a non-empty decimal EditTextPreference
    private void setupDecimalEditTextPreference(EditTextPreference preference) {
        if (preference != null) {
            preference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) pref ->
                    getResources().getString(R.string.setting_current_value) + pref.getText());
            preference.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                editText.setSelection(editText.length());
            });
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue.toString().trim().isEmpty()) {
                    GoUtils.DisplayToast(this.getContext(), getResources().getString(R.string.app_error_input_null));
                    return false;
                }
                return true;
            });
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_main);
        stylePreferences(getPreferenceScreen());
        Preference hero = new Preference(requireContext());
        hero.setKey("settings_hero");
        hero.setOrder(-1);
        hero.setTitle("你的定位偏好");
        hero.setSummary("移动、位置与更新，按你的习惯调整");
        hero.setSelectable(false);
        hero.setLayoutResource(R.layout.preference_hero);
        getPreferenceScreen().addPreference(hero);

        // 设置版本号
        String verName;
        verName = GoUtils.getVersionName(FragmentSettings.this.getContext());
        Preference pfVersion = findPreference("setting_version");
        if (pfVersion != null) {
            pfVersion.setSummary(verName);
        }

        ListPreference pfJoystick = findPreference("setting_joystick_type");
        if (pfJoystick != null) {
            // 使用自定义 SummaryProvider
            pfJoystick.setSummaryProvider((Preference.SummaryProvider<ListPreference>) preference -> getResources().getString(R.string.setting_current_value) + Objects.requireNonNull(preference.getEntry()));
            pfJoystick.setOnPreferenceChangeListener((preference, newValue) -> newValue.toString().trim().length() != 0);
        }

        EditTextPreference pfWalk = findPreference("setting_walk");
        setupDecimalEditTextPreference(pfWalk);

        EditTextPreference pfRun = findPreference("setting_run");
        setupDecimalEditTextPreference(pfRun);

        EditTextPreference pfBike = findPreference("setting_bike");
        setupDecimalEditTextPreference(pfBike);

        EditTextPreference pfAltitude = findPreference("setting_altitude");
        setupDecimalEditTextPreference(pfAltitude);

        SwitchPreferenceCompat pLog = findPreference("setting_log_off");
        if (pLog != null) {
            pLog.setOnPreferenceChangeListener((preference, newValue) -> {
                if(((SwitchPreferenceCompat) preference).isChecked() != (Boolean) newValue) {
                    XLog.d(preference.getKey() + newValue);

                    if (Boolean.parseBoolean(newValue.toString())) {
                        XLog.d("on");
                    } else {
                        XLog.d("off");
                    }
                    return true;
                } else {
                    return false;
                }
            });
        }

        EditTextPreference pfPosHisValid = findPreference("setting_pos_history");
        setupDecimalEditTextPreference(pfPosHisValid);

        EditTextPreference pfLatOffset = findPreference("setting_lat_max_offset");
        setupDecimalEditTextPreference(pfLatOffset);

        EditTextPreference pfLonOffset = findPreference("setting_lon_max_offset");
        setupDecimalEditTextPreference(pfLonOffset);
    }
    private void stylePreferences(androidx.preference.PreferenceGroup group) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            if (preference instanceof androidx.preference.PreferenceCategory) {
                preference.setLayoutResource(R.layout.preference_section);
                stylePreferences((androidx.preference.PreferenceGroup) preference);
            } else {
                preference.setLayoutResource(R.layout.preference_card);
                if (preference instanceof EditTextPreference || preference instanceof ListPreference) {
                    preference.setWidgetLayoutResource(R.layout.preference_chevron);
                }
                if ("setting_version".equals(preference.getKey()) || "setting_author".equals(preference.getKey())) {
                    preference.setSelectable(false);
                }
            }
        }
    }

    @Override
    public void onViewCreated(@androidx.annotation.NonNull android.view.View view, Bundle state) {
        super.onViewCreated(view, state);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        getListView().setPadding(padding, padding, padding, padding * 2);
        getListView().setClipToPadding(false);
        setDivider(null);
        setDividerHeight(0);
    }
}
