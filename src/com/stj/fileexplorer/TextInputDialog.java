/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * This file is part of FileExplorer.
 *
 * FileExplorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileExplorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.stj.fileexplorer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.stj.fileexplorer.R;


public class TextInputDialog extends AlertDialog implements TextWatcher {
    private String mInputText;
    private String mTitle;
    private String mMsg;
    private OnFinishListener mListener;
    private Context mContext;
    private View mView;
    private EditText mEditText;
    private int MAX_FOLDER_NAME_LENGTH = 180;

    public interface OnFinishListener {
        // return true to accept and dismiss, false reject
        boolean onFinish(String text);
    }

    public TextInputDialog(Context context, String title, String msg, String text, OnFinishListener listener) {
        super(context);
        mTitle = title;
        mMsg = msg;
        mListener = listener;
        mInputText = text;
        mContext = context;
    }

    public String getInputText() {
        return mInputText;
    }

    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.textinput_dialog, null);

        setTitle(mTitle);
        setMessage(mMsg);

        mEditText = (EditText) mView.findViewById(R.id.text);
        mEditText.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(MAX_FOLDER_NAME_LENGTH)
        });
        mEditText.setText(mInputText);

        setView(mView);
        setButton(BUTTON_POSITIVE, mContext.getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == BUTTON_POSITIVE) {
                            mInputText = mEditText.getText().toString();
                            if (mListener.onFinish(mInputText)) {
                                mEditText.removeTextChangedListener(TextInputDialog.this);
                                dismiss();
                            }
                        }
                    }
                });
        setButton(BUTTON_NEGATIVE, mContext.getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mEditText.removeTextChangedListener(TextInputDialog.this);
                        dismiss();
                    }
                });

        mEditText.addTextChangedListener(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        Button positiveBtn = this.getButton(BUTTON_POSITIVE);
        if (positiveBtn != null) {
            if (mEditText.length() >= MAX_FOLDER_NAME_LENGTH) {
                positiveBtn.setEnabled(false);
                Util.showPrompt(mContext, R.string.prompt_max_limit_length, Toast.LENGTH_LONG);
            } else {
                if (!positiveBtn.isEnabled()) {
                    positiveBtn.setEnabled(true);
                }
            }
        }
    }
}
