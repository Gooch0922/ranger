/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.usergroupsync;

import org.apache.ranger.unixusersync.config.UserGroupSyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserGroupSync implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(UserGroupSync.class);

    private UserGroupSink   ugSink;
    private UserGroupSource ugSource;

    public static void main(String[] args) {
        UserGroupSync userGroupSync = new UserGroupSync();

        userGroupSync.run();
    }

    @Override
    public void run() {
        try {
            long    sleepTimeBetweenCycleInMillis     = UserGroupSyncConfig.getInstance().getSleepTimeInMillisBetweenCycle();
            long    initSleepTimeBetweenCycleInMillis = UserGroupSyncConfig.getInstance().getInitSleepTimeInMillisBetweenCycle();
            boolean initPending                       = true;

            while (initPending) {
                try {
                    if (UserGroupSyncConfig.isUgsyncServiceActive()) {
                        ugSink = UserGroupSyncConfig.getInstance().getUserGroupSink();

                        LOG.info("initializing sink: {}", ugSink.getClass().getName());

                        ugSink.init();

                        ugSource = UserGroupSyncConfig.getInstance().getUserGroupSource();

                        LOG.info("initializing source: {}", ugSource.getClass().getName());

                        ugSource.init();

                        LOG.info("Begin: initial load of user/group from source ==> sink");

                        syncUserGroup();

                        LOG.info("End: initial load of user/group from source ==> sink");

                        initPending = false;

                        LOG.info("Done initializing user/group source and sink");
                    } else {
                        LOG.debug("Sleeping for [{}] milliSeconds as this server is running in passive mode", initSleepTimeBetweenCycleInMillis);

                        Thread.sleep(initSleepTimeBetweenCycleInMillis);
                    }
                } catch (Throwable t) {
                    LOG.error("Failed to initialize UserGroup source/sink. Will retry after {} milliseconds. Error details: ", sleepTimeBetweenCycleInMillis, t);

                    try {
                        LOG.debug("Sleeping for [{}] milliSeconds", sleepTimeBetweenCycleInMillis);

                        Thread.sleep(sleepTimeBetweenCycleInMillis);
                    } catch (Exception e) {
                        LOG.error("Failed to wait for [{}] milliseconds before attempting to initialize UserGroup source/sink", sleepTimeBetweenCycleInMillis, e);
                    }
                }
            }

            while (true) {
                try {
                    LOG.debug("Sleeping for {} milliSeconds", sleepTimeBetweenCycleInMillis);

                    Thread.sleep(sleepTimeBetweenCycleInMillis);
                } catch (InterruptedException e) {
                    LOG.error("Failed to wait for [{}] milliseconds before attempting to synchronize UserGroup information", sleepTimeBetweenCycleInMillis, e);
                }

                try {
                    if (UserGroupSyncConfig.isUgsyncServiceActive()) {
                        LOG.info("Begin: update user/group from source ==> sink");

                        syncUserGroup();

                        LOG.info("End: update user/group from source ==> sink");
                    } else {
                        LOG.info("Sleeping for [{}] milliSeconds as this server is running in passive mode", sleepTimeBetweenCycleInMillis);
                    }
                } catch (Throwable t) {
                    LOG.error("Failed to synchronize UserGroup information. Error details: ", t);
                }
            }
        } catch (Throwable t) {
            LOG.error("UserGroupSync thread got an error ", t);
        } finally {
            LOG.info("Shutting down the UserGroupSync thread");
        }
    }

    private void syncUserGroup() throws Throwable {
        UserGroupSyncConfig config = UserGroupSyncConfig.getInstance();

        if (config.isUserSyncEnabled()) {
            ugSource.updateSink(ugSink);
        }
    }
}
