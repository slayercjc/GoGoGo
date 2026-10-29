package com.zcshou.gogogo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.zcshou.utils.GoUtils;

import java.util.ArrayList;

public class WelcomeActivity extends BaseActivity {
    private static SharedPreferences preferences;
    private static final String KEY_ACCEPT_AGREEMENT = "KEY_ACCEPT_AGREEMENT";
    private static final String KEY_ACCEPT_PRIVACY = "KEY_ACCEPT_PRIVACY";

    private boolean isPermission = false;
    private static final int SDK_PERMISSION_REQUEST = 127;
    private final ArrayList<String> ReqPermissions = new ArrayList<>();

    private CheckBox checkBox;
    private Boolean mAgreement;
    private Boolean mPrivacy;
    private boolean pendingEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);
        preferences = getSharedPreferences(KEY_ACCEPT_AGREEMENT, MODE_PRIVATE);
        boolean accepted = preferences.getBoolean(KEY_ACCEPT_AGREEMENT, false)
                && preferences.getBoolean(KEY_ACCEPT_PRIVACY, false);
        // Existing users have already completed the old welcome flow.
        if (!preferences.contains("onboarding_complete")) {
            preferences.edit().putBoolean("onboarding_complete", accepted).apply();
        }
        if (accepted && preferences.getBoolean("onboarding_complete", false)) {
            openMainActivity();
            return;
        }
        pendingEntry = savedInstanceState != null && savedInstanceState.getBoolean("pendingEntry", false);
        setContentView(R.layout.activity_welcome);

        Button startBtn = findViewById(R.id.startButton);
        startBtn.setOnClickListener(v -> startMainActivity());

        checkAgreementAndPrivacy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SDK_PERMISSION_REQUEST) {
            isPermission = hasLocationPermission();
            if (isPermission && pendingEntry) {
                pendingEntry = false;
                openMainActivity();
            } else if (!isPermission) {
                pendingEntry = false;
                GoUtils.DisplayToast(this, getString(R.string.app_error_permission));
            }
        }
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkDefaultPermissions() {
        ReqPermissions.clear();
        isPermission = hasLocationPermission();
        if (!isPermission) {
            ReqPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            ReqPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !getPreferences(MODE_PRIVATE).getBoolean("notificationAsked", false)) {
            ReqPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
            getPreferences(MODE_PRIVATE).edit().putBoolean("notificationAsked", true).apply();
        }
        if (!ReqPermissions.isEmpty()) {
            requestPermissions(ReqPermissions.toArray(new String[0]), SDK_PERMISSION_REQUEST);
        }
    }

    private void startMainActivity() {
        if (!checkBox.isChecked()) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_agreement));
            return;
        }

        if (hasLocationPermission()) {
            openMainActivity();
        } else {
            pendingEntry = true;
            checkDefaultPermissions();
        }
    }

    private void openMainActivity() {
        preferences.edit().putBoolean("onboarding_complete", true).apply();
        ((GoApplication) getApplication()).initializeBaidu();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle state) {
        state.putBoolean("pendingEntry", pendingEntry);
        super.onSaveInstanceState(state);
    }

    private void doAcceptation() {
        if (mAgreement && mPrivacy) {
            checkBox.setChecked(true);
            checkDefaultPermissions();
        } else {
            checkBox.setChecked(false);
        }
        //实例化Editor对象
        SharedPreferences.Editor editor = preferences.edit();
        //存入数据
        editor.putBoolean(KEY_ACCEPT_AGREEMENT, mAgreement);
        editor.putBoolean(KEY_ACCEPT_PRIVACY, mPrivacy);
        //提交修改
        editor.apply();
    }

    private void showAgreementDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setContentView(R.layout.user_agreement);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            TextView tvContent = window.findViewById(R.id.tv_content);
            Button tvCancel = window.findViewById(R.id.tv_cancel);
            Button tvAgree = window.findViewById(R.id.tv_agree);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(getResources().getString(R.string.app_agreement_content));
            tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            tvContent.setText(ssb, TextView.BufferType.SPANNABLE);

            tvCancel.setOnClickListener(v -> {
                mAgreement = false;

                doAcceptation();

                alertDialog.cancel();
            });

            tvAgree.setOnClickListener(v -> {
                mAgreement = true;

                doAcceptation();

                alertDialog.cancel();
            });
        }
    }

    private void showPrivacyDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setContentView(R.layout.user_privacy);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            TextView tvContent = window.findViewById(R.id.tv_content);
            Button tvCancel = window.findViewById(R.id.tv_cancel);
            Button tvAgree = window.findViewById(R.id.tv_agree);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(getResources().getString(R.string.app_privacy_content));
            tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            tvContent.setText(ssb, TextView.BufferType.SPANNABLE);

            tvCancel.setOnClickListener(v -> {
                mPrivacy = false;

                doAcceptation();

                alertDialog.cancel();
            });

            tvAgree.setOnClickListener(v -> {
                mPrivacy = true;

                doAcceptation();

                alertDialog.cancel();
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void checkAgreementAndPrivacy() {
        preferences = getSharedPreferences(KEY_ACCEPT_AGREEMENT, MODE_PRIVATE);
        mPrivacy = preferences.getBoolean(KEY_ACCEPT_PRIVACY, false);
        mAgreement = preferences.getBoolean(KEY_ACCEPT_AGREEMENT, false);

        checkBox = findViewById(R.id.check_agreement);
        // 拦截 CheckBox 的点击事件
        checkBox.setOnTouchListener((v, event) -> {
            if (v instanceof TextView) {
                TextView text = (TextView) v;
                MovementMethod method = text.getMovementMethod();
                if (method != null && text.getText() instanceof Spannable
                        && event.getAction() == MotionEvent.ACTION_UP) {
                    if (method.onTouchEvent(text, (Spannable) text.getText(), event)) {
                        event.setAction(MotionEvent.ACTION_CANCEL);
                    }
                }
            }
            return false;
        });
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!mPrivacy || !mAgreement) {
                    GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_read));
                    checkBox.setChecked(false);
                }
            } else {
                mPrivacy = false;
                mAgreement = false;
            }
        });

        String str = getString(R.string.app_agreement_privacy);
        SpannableStringBuilder builder = getSpannableStringBuilder(str);

        checkBox.setText(builder);
        checkBox.setMovementMethod(LinkMovementMethod.getInstance());

        if (mPrivacy && mAgreement) {
            checkBox.setChecked(true);
            checkDefaultPermissions();
        } else {
            checkBox.setChecked(false);
        }
    }

    @NonNull
    private SpannableStringBuilder getSpannableStringBuilder(String str) {
        SpannableStringBuilder builder = new SpannableStringBuilder(str);
        ClickableSpan clickSpanAgreement = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showAgreementDialog();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(getResources().getColor(R.color.colorPrimary, WelcomeActivity.this.getTheme()));
                ds.setUnderlineText(false);
            }
        };
        int agreement_start = str.indexOf("《");
        int agreement_end = str.indexOf("》") + 1;
        builder.setSpan(clickSpanAgreement, agreement_start,agreement_end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ClickableSpan clickSpanPrivacy = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showPrivacyDialog();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(getResources().getColor(R.color.colorPrimary, WelcomeActivity.this.getTheme()));
                ds.setUnderlineText(false);
            }
        };
        int privacy_start = str.indexOf("《", agreement_end);
        int privacy_end = str.indexOf("》", agreement_end) + 1;
        builder.setSpan(clickSpanPrivacy, privacy_start, privacy_end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }
}

