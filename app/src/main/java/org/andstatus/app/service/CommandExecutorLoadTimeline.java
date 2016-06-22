/* 
 * Copyright (c) 2014 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.service;

import android.database.sqlite.SQLiteConstraintException;

import org.andstatus.app.net.http.ConnectionException;
import org.andstatus.app.util.MyLog;

class CommandExecutorLoadTimeline extends CommandExecutorStrategy {
    
    @Override
    void execute() {
        try {
            if (execContext.getMyAccount().getConnection().isApiSupported(execContext.getTimelineType().getConnectionApiRoutine())) {
                MyLog.d(this, "Getting " + execContext.getCommandData().toCommandSummary(execContext.getMyContext()) +
                        " by " + execContext.getMyAccount().getAccountName() );
                TimelineDownloader downloader = TimelineDownloader.getStrategy(execContext);
                downloader.download();
                downloader.onSyncEnded();
            } else {
                MyLog.v(this, execContext.getTimelineType() + " is not supported for "
                        + execContext.getMyAccount().getAccountName());
            }
            logOk(true);
        } catch (ConnectionException e) {
            logConnectionException(e, "Load Timeline");
        } catch (SQLiteConstraintException e) {
            MyLog.e(this, execContext.getTimelineType().toString(), e);
        }
    }
}
