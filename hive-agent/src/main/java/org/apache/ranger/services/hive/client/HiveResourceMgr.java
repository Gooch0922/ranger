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
package org.apache.ranger.services.hive.client;

import org.apache.ranger.plugin.client.HadoopException;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.plugin.util.TimedEventUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class HiveResourceMgr {
    private static final Logger LOG = LoggerFactory.getLogger(HiveResourceMgr.class);

    private static final String DATABASE = "database";
    private static final String TABLE    = "table";
    private static final String COLUMN   = "column";

    private HiveResourceMgr() {
        // to block instantiation
    }

    public static Map<String, Object> connectionTest(String serviceName, Map<String, String> configs) throws Exception {
        LOG.debug("==> HiveResourceMgr.connectionTest ServiceName: {}Configs{}", serviceName, configs);

        Map<String, Object> ret;

        try {
            ret = HiveClient.connectionTest(serviceName, configs);
        } catch (HadoopException e) {
            LOG.error("<== HiveResourceMgr.connectionTest Error: {}", String.valueOf(e));

            throw e;
        }

        LOG.debug("<== HiveResourceMgr.connectionTest Result : {}", ret);

        return ret;
    }

    public static List<String> getHiveResources(String serviceName, String serviceType, Map<String, String> configs, ResourceLookupContext context) throws Exception {
        String                    userInput    = context.getUserInput();
        String                    resource     = context.getResourceName();
        Map<String, List<String>> resourceMap  = context.getResources();
        List<String>              resultList   = null;
        List<String>              databaseList = null;
        List<String>              tableList    = null;
        List<String>              columnList   = null;
        String                    databaseName = null;
        String                    tableName    = null;
        String                    columnName   = null;

        LOG.debug("<== HiveResourceMgr.getHiveResources()  UserInput: \"{}\" resource : {} resourceMap: {}", userInput, resource, resourceMap);

        if (userInput != null && resource != null) {
            if (resourceMap != null && !resourceMap.isEmpty()) {
                databaseList = resourceMap.get(DATABASE);
                tableList    = resourceMap.get(TABLE);
                columnList   = resourceMap.get(COLUMN);
            }

            switch (resource.trim().toLowerCase()) {
                case DATABASE:
                    databaseName = userInput;
                    break;
                case TABLE:
                    tableName = userInput;
                    break;
                case COLUMN:
                    columnName = userInput;
                    break;
                default:
                    break;
            }
        }

        if (serviceName != null && userInput != null) {
            try {
                LOG.debug("==> HiveResourceMgr.getHiveResources() UserInput: {} configs: {} databaseList: {} tableList: {} columnList: {}", userInput, configs, databaseList, tableList, columnList);

                final HiveClient hiveClient = new HiveConnectionMgr().getHiveConnection(serviceName, serviceType, configs);

                if (hiveClient != null) {
                    final Callable<List<String>> callableObj;
                    final String                 finalDbName;
                    final String                 finalColName;
                    final String                 finalTableName;
                    final List<String>           finaldatabaseList = databaseList;
                    final List<String>           finaltableList    = tableList;
                    final List<String>           finalcolumnList   = columnList;

                    if (databaseName != null && !databaseName.isEmpty()) {
                        // get the DBList for given Input
                        databaseName += "*";
                        finalDbName  = databaseName;

                        callableObj = () -> hiveClient.getDatabaseList(finalDbName, finaldatabaseList);
                    } else if (tableName != null && !tableName.isEmpty()) {
                        // get  ColumnList for given Input
                        tableName      += "*";
                        finalTableName = tableName;
                        callableObj    = () -> hiveClient.getTableList(finalTableName, finaldatabaseList, finaltableList);
                    } else if (columnName != null && !columnName.isEmpty()) {
                        // get  ColumnList for given Input
                        columnName += "*";
                        finalColName   = columnName;
                        finalDbName    = databaseName;
                        finalTableName = tableName;

                        callableObj = () -> hiveClient.getColumnList(finalColName, finaldatabaseList, finaltableList, finalcolumnList);
                    } else {
                        callableObj = null;
                    }

                    if (callableObj != null) {
                        synchronized (hiveClient) {
                            resultList = TimedEventUtil.timedTask(callableObj, 5, TimeUnit.SECONDS);
                        }
                    } else {
                        LOG.error("Could not initiate at timedTask");
                    }
                }
            } catch (Exception e) {
                LOG.error("Unable to get hive resources.", e);

                throw e;
            }
        }

        LOG.debug("<== HiveResourceMgr.getHiveResources() UserInput: {} configs: {} databaseList: {} tableList: {} columnList: {}Result :{}", userInput, configs, databaseList, tableList, columnList, resultList);

        return resultList;
    }
}
