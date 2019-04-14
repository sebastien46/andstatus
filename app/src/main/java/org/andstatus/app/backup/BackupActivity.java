/*
 * Copyright (C) 2019 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app.backup;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.andstatus.app.MyActivity;
import org.andstatus.app.R;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.os.AsyncTaskLauncher;
import org.andstatus.app.os.MyAsyncTask;
import org.andstatus.app.util.FileUtils;
import org.andstatus.app.util.MyLog;
import org.andstatus.app.util.Permissions;
import org.andstatus.app.util.SimpleFileDialog;

import java.io.File;

import androidx.annotation.NonNull;

public class BackupActivity extends MyActivity implements ProgressLogger.ProgressCallback {
    File backupFolder = null;
    BackupTask asyncTask = null;
    private int progressCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLayoutId = R.layout.backup;
        super.onCreate(savedInstanceState);

        Permissions.checkPermissionAndRequestIt(this, Permissions.PermissionType.WRITE_EXTERNAL_STORAGE);

        findViewById(R.id.button_backup).setOnClickListener(this::doBackup);
        findViewById(R.id.backup_folder).setOnClickListener(this::selectBackupFolder);
        findViewById(R.id.button_select_backup_folder).setOnClickListener(this::selectBackupFolder);
        showBackupFolder();
    }

    private void doBackup(View v) {
        if (asyncTask == null || asyncTask.completedBackgroundWork()) {
            resetProgress();
            asyncTask = new BackupTask(BackupActivity.this);
            new AsyncTaskLauncher<File>().execute(this, true, asyncTask, getBackupFolder());
        }
    }

    private void selectBackupFolder(View v) {
        new SimpleFileDialog(BackupActivity.this,
                    SimpleFileDialog.TypeOfSelection.FOLDER_CHOOSE,
                    chosenFolder -> setBackupFolder(new File(chosenFolder)))
                .chooseFileOrDir(getBackupFolder().getAbsolutePath());
    }

    @NonNull
    private File getBackupFolder() {
        File folder;
        if (backupFolder != null && backupFolder.exists()) {
            folder = backupFolder;
        } else {
            folder = MyBackupManager.getDefaultBackupFolder(this);
        }
        if (!folder.exists() || !folder.isDirectory()) {
            folder = new File(FileUtils.ROOT_FOLDER);
        }
        return folder;
    }

    void setBackupFolder(File backupFolder) {
        if ( backupFolder == null ) {
            MyLog.d(this, "No backup folder selected");
            return;
        } else if ( backupFolder.exists() ) {
            if (!backupFolder.isDirectory()) {
                MyLog.d(this, "Is not a folder '" + backupFolder.getAbsolutePath() + "'");
                return;
            }
        } else {
            MyLog.i(this, "The folder doesn't exist: '" + backupFolder.getAbsolutePath() + "'");
            return;
        }
        this.backupFolder = backupFolder;
        showBackupFolder();
        resetProgress();
    }

    private void showBackupFolder() {
        TextView view = findViewById(R.id.backup_folder);
        if (view != null) {
            view.setText(getBackupFolder().getAbsolutePath());
        }
    }

    private static class BackupTask extends MyAsyncTask<File, CharSequence, Void> {
        private final BackupActivity activity;

        BackupTask(BackupActivity activity) {
            super(PoolEnum.LONG_UI);
            this.activity = activity;
        }

        @Override
        protected Void doInBackground2(File... params) {
            MyBackupManager.backupInteractively(params[0], activity, activity);
            return null;
        }
    }

    private void resetProgress() {
        progressCounter = 0;
        TextView progressLog = findViewById(R.id.progress_log);
        progressLog.setText("");
    }

    private void addProgressMessage(CharSequence message) {
        progressCounter++;
        TextView progressLog = findViewById(R.id.progress_log);
        String log = Integer.toString(progressCounter) + ". " + message + "\n" + progressLog.getText();
        progressLog.setText(log);
    }

    @Override
    protected void onResume() {
        MyContextHolder.get().setInForeground(true);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyContextHolder.get().setInForeground(false);
    }

    @Override
    public void onProgressMessage(CharSequence message) {
        runOnUiThread( () -> addProgressMessage(message));
    }
}
