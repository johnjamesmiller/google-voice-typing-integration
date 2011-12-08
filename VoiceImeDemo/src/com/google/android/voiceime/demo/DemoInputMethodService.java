/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.voiceime.demo;

import com.google.android.voiceime.VoiceRecognitionTrigger;

import android.app.Service;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Demo IME that triggers voice recognition. <br>
 * This is a simplified IME that shows how to integrate voice recognition into
 * an IME. Based on the Android OS version, and on the applications that are
 * installed on the device, voice recognition will be done in two different
 * ways: using a Google voice typing IME, or by triggering an {@link Intent}. <br>
 * Google voice typing is the preferred option, as it provides a better
 * recognition experience (streaming results), and corrections. If it is not
 * installed, the fall-back is to use voice recognition by triggering an Intent.
 * How the recognition is done is transparent for the IME. <br>
 * To integrate the IME, the main steps are:
 * <ul>
 * <li>add a microphone icon into the IME. You should use the assets included in
 * this demo app.
 * <li>create a {@link VoiceRecognitionTrigger} when the IME is created, and
 * destroy it when the IME is destroyed.
 * <li>call {@link VoiceRecognitionTrigger#startVoiceRecognition(String)} when
 * the user clicks on the microphone icon.
 * <li>when the IME starts, you should call
 * {@link VoiceRecognitionTrigger#onStartInputView()}. This call pastes the last
 * recognition result into the {@link TextView} if necessary.
 * <ul>
 * <br>
 * The microphone icon should reflect the status of Voice IME:
 * <ul>
 * <li>no microphone when no voice recognition is installed (
 * {@link VoiceRecognitionTrigger#isInstalled()}).
 * <li>a greyed-out microphone when voice recognition is installed, but it cannot
 * be used ({@link VoiceRecognitionTrigger#isEnabled()})
 * <li>the standard microphone when voice recognition is installed, and
 * enabled.
 * </ul>
 */
public class DemoInputMethodService extends InputMethodService {

    private static final String TAG = "DemoInputMethodService";

    private ImageButton mButton;

    private View mView;

    private TextView mText;

    private VoiceRecognitionTrigger mVoiceRecognitionTrigger;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "#onCreate");

        // Create the voice recognition trigger, and register the listener.
        // The trigger has to unregistered, when the IME is destroyed.
        mVoiceRecognitionTrigger = new VoiceRecognitionTrigger(this);
        mVoiceRecognitionTrigger.register(new VoiceRecognitionTrigger.Listener() {

            @Override
            public void onVoiceImeEnabledStatusChange() {
                // The call back is done on the main thread.
                updateVoiceImeStatus();
            }
        });
    }

    @Override
    public View onCreateInputView() {
        Log.i(TAG, "#onCreateInputView");
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Service.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.ime, null);

        mText= (TextView) mView.findViewById(R.id.message);

        mButton = (ImageButton) mView.findViewById(R.id.mic_button);
        if (mVoiceRecognitionTrigger.isInstalled()) {

            // Voice recognition is installed on the phone, and the onClick listener is set.
            // When voice recognition is triggered, the IME should pass its language, so
            // voice recognition will be done in the same language. The language should be
            // specified in Java locale format.
            // If the IME does not have a language, the IME should call
            // mVoiceRecognitionTrigger.startVoiceRecognition() without any parameter.
            mButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mVoiceRecognitionTrigger.startVoiceRecognition(getImeLanguage());
                }
            });

            // The status of the IME (i.e., installed and enabled, installed and displayed)
            // is updated.
            updateVoiceImeStatus();
        } else {

            // No voice recognition is installed, and the microphone icon is not displayed.
            mText.setText(R.string.api_not_available);
            mButton.setVisibility(View.GONE);
        }
        return mView;
    }

    /**
     * Returns the language of the IME. The langauge is used in voice recognition to match the
     * current language of the IME.
     */
    private String getImeLanguage() {
        return "en-US";
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        Log.i(TAG, "#onStartInputView");
        super.onStartInputView(info, restarting);
        if (mVoiceRecognitionTrigger != null) {
            // This method call is required for pasting the recognition results into the TextView
            // when the recognition is done using the Intent API.
            mVoiceRecognitionTrigger.onStartInputView();
        }
    }

    /**
     * Update the microphone icon to reflect the status of the voice recognition.
     */
    private void updateVoiceImeStatus() {
        if (mButton == null) {
            return;
        }

        if (mVoiceRecognitionTrigger.isInstalled()) {
            mButton.setVisibility(View.VISIBLE);
            if (mVoiceRecognitionTrigger.isEnabled()) {
                // Voice recognition is installed and enabled.
                mButton.setEnabled(true);
            } else {
                // Voice recognition is installed, but it is not enabled (no network).
                // The microphone icon is displayed greyed-out.
                mButton.setEnabled(false);
            }
        } else {
            // Voice recognition is not installed, and the microphone icon is not displayed.
            mButton.setVisibility(View.GONE);
        }
        mView.invalidate();
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "#onDestroy");
        if (mVoiceRecognitionTrigger != null) {
            // To avoid service leak, the trigger has to be unregistered when
            // the IME is destroyed.
            mVoiceRecognitionTrigger.unregister(this);
        }
        super.onDestroy();
    }
}
