/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs.webdav;

import org.apache.jackrabbit.webdav.security.CurrentUserPrivilegeSetProperty;
import org.apache.jackrabbit.webdav.security.Privilege;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.security.UnixUserGroupInformation;

import java.util.List;
import java.util.ArrayList;


public class UtilsHelper {

    public static CurrentUserPrivilegeSetProperty getCurrentUserPrivilegeSetProperty(FileStatus fstat, UnixUserGroupInformation ugi) {

        List<Privilege> list = new ArrayList<Privilege>();

        if (ugi.getUserName().equals(fstat.getOwner())) {
            FsAction action = fstat.getPermission().getUserAction();
            extractPrivileges(list, action);
        } else if (groupExists(ugi.getGroupNames(), fstat.getGroup())) {
            FsAction action = fstat.getPermission().getGroupAction();
            extractPrivileges(list, action);
        } else {
            FsAction action = fstat.getPermission().getOtherAction();
            extractPrivileges(list, action);
        }

        if (list.size() > 0) {
            Privilege[] allPrivs = list.toArray(new Privilege[0]);
            return new CurrentUserPrivilegeSetProperty(allPrivs);
        }

        return new CurrentUserPrivilegeSetProperty(new Privilege[0]);
    }

    private static void extractPrivileges(List<Privilege> list, FsAction action) {
        if (action.implies(FsAction.READ)) {
            list.add(Privilege.PRIVILEGE_READ);
        }
        if (action.implies(FsAction.WRITE)) {
            list.add(Privilege.PRIVILEGE_WRITE);
        }
    }

    private static boolean groupExists(String[] groups, String group) {
        for (String g : groups) {
            if (g.equals(group)) {
                return true;
            }
        }
        return false;
    }

}
