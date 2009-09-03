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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.AccessControlException;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.security.UnixUserGroupInformation;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.security.*;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.apache.jackrabbit.webdav.simple.ResourceConfig;

import javax.security.auth.login.LoginException;
import javax.jcr.RepositoryException;

public class FSDavResource implements DavResource {

    private static final Log LOG = LogFactory.getLog(FSDavResource.class);

    private static final String COMPLIANCE_CLASS = 
        DavCompliance.concatComplianceClasses(new String[] {DavCompliance._2_});

    //We support compliance level 1, and the listed methods. PROPFIND, PROPPATCH
    //are not supported for now. 
    private static final String SUPPORTED_METHODS 
        = "OPTIONS, GET, HEAD, POST, TRACE, MKCOL, COPY, PUT, DELETE, MOVE, PROPFIND";

    private FSDavResourceFactory factory;
    private final DavResourceLocator locator;
    private LockManager lockManager = new SimpleLockManager();
    private DavSession session;

    private DavPropertySet properties = new DavPropertySet();

    //hadoop objects
    private final Configuration conf;
    private final FileSystem fs;
    private final Path path; //the path object that this resource represents
    private boolean inited = false;

    /**
     * This only indicates that the DavResource is to be created as a file 
     * or directory
     * @see isCollection
     */
    private boolean isCollectionRequest = false;

    public FSDavResource(FSDavResourceFactory factory,
                         DavResourceLocator locator,
                         DavSession session,
                         ResourceConfig resourceConfig,
                         Configuration conf,
                         boolean isCollectionRequest) throws IOException {

        this.factory = factory;
        this.locator = locator;
        this.session = session;
        this.conf = conf;
        this.fs = FileSystem.get(conf);
        String pathStr = URLDecoder.decode(locator.getResourcePath());
        if (pathStr.trim().equals("")) { //empty path is not allowed
            pathStr = "/";
        }
        this.path = new Path(pathStr);
        this.isCollectionRequest = isCollectionRequest;
    }

    public FSDavResource(FSDavResourceFactory factory,
                         DavResourceLocator locator,
                         DavSession session,
                         ResourceConfig resourceConfig,
                         Configuration conf) throws IOException {

        this(factory, locator,session, resourceConfig, conf, false);
    }

    public String getComplianceClass() {
        return COMPLIANCE_CLASS;
    }

    private Path getPath() {
        return path;
    }

    public void addLockManager(LockManager lockmgr) {
        this.lockManager = lockmgr;
    }

    public void addMember(DavResource resource, InputContext inputContext)
        throws DavException {
        //A PUT performed on an existing resource replaces the GET response entity of the resource. Properties
        //defined on the resource may be recomputed during PUT processing but are not otherwise affected.
        Path destPath = ((FSDavResource)resource).getPath();
        try {
            FSDavResource dfsResource = (FSDavResource)resource;
            if (dfsResource.isCollectionRequest) {
                LOG.debug("creating new directory : " + destPath.toUri().getPath());
                boolean success = fs.mkdirs(destPath);
                if (!success) {
                    throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                if (!inputContext.hasStream() || inputContext.getContentLength() < 0) {
                    LOG.debug("creating new file : " + destPath.toUri().getPath());
                    boolean success = fs.createNewFile(destPath);
                    if (!success) {
                        throw new DavException(DavServletResponse.SC_CONFLICT);
                    }
                } else {
                    LOG.debug("writing new file : " + destPath.toUri().getPath());
                    OutputStream out = fs.create(destPath);
                    InputStream in = inputContext.getInputStream();
                    IOUtils.copyBytes(in, out, conf, true);
                }
            }
        } catch (IOException ex) {
            LOG.warn(StringUtils.stringifyException(ex));
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public void removeMember(DavResource member) throws DavException {
        try {
            Path destPath = ((FSDavResource)member).getPath();
            boolean success = fs.delete(destPath, true);
            LOG.info("Delete " + destPath.toString() + ": " + success);
            if (!success) {
                throw new DavException(DavServletResponse.SC_NOT_FOUND);
            }
        } catch (IOException ex) {
            LOG.warn(StringUtils.stringifyException(ex));
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public void copy(DavResource destination, boolean shallow) throws DavException {

        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (!shallow || !isCollection()) {
            FSDavResource dest = (FSDavResource)destination;
            try {
                FileUtil.copy(fs, path, fs,dest.getPath(), false, conf);
            } catch (IOException ex) {
                LOG.warn(StringUtils.stringifyException(ex));
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        // TODO: currently no support for shallow copy; however this is
        // only relevant if the source resource is a collection, because
        // otherwise it doesn't make a difference
        throw new DavException(DavServletResponse.SC_FORBIDDEN, "Unable to perform shallow copy.");
    }

    public void move(DavResource destination) throws DavException {
        try {
            fs.rename(path, ((FSDavResource)destination).getPath());
        } catch (IOException ex) {
            LOG.warn(StringUtils.stringifyException(ex));
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public boolean exists() {
        try {
            return fs.exists(path);
        } catch (IOException ex) {
            // a DAV exception cannot be thrown
            LOG.warn(StringUtils.stringifyException(ex));
            throw new RuntimeException(ex);
        }
    }

    public DavResource getCollection() {
        if (path.depth() == 0) {
            return null;
        }

        DavResourceLocator newLocator = locator.getFactory().createResourceLocator(locator.getPrefix(),
                                                                                   path.getParent().toUri().getPath());
        try {
            return factory.createResource(newLocator, getSession());
        } catch (DavException ex) {
            LOG.warn(StringUtils.stringifyException(ex));
            throw new RuntimeException(ex);
        }
    }

    public String getDisplayName() {
        LOG.info("DISPLAY_NAME: " + path.getName());

        return path.getName();
    }

    public DavResourceFactory getFactory() {
        return factory;
    }

    public String getHref() {
        StringBuffer buffer = new StringBuffer();
        Path p = this.path;

        while (p != null && !("".equals(p.getName()))) {
            buffer.insert(0, p.getName());
            buffer.insert(0, "/");
            p = p.getParent();
        }

        if (0 == buffer.length()) {
            buffer.insert(0, "/");            
        }

        LOG.info("HREF: " + buffer.toString());
        return buffer.toString();
    }

    public DavResourceLocator getLocator() {
        return locator;
    }

    public ActiveLock getLock(Type type, Scope scope) {
        return lockManager.getLock(type, scope, this);
    }

    public ActiveLock[] getLocks() {
        return new ActiveLock[0];
    }

    public DavResourceIterator getMembers() {
        ArrayList<DavResource> list = new ArrayList<DavResource>();
        try {
            FileStatus[] statuses = fs.listStatus(path);
            if (statuses != null) {
                for (FileStatus s:statuses) {
                    Path p = s.getPath();
                    LOG.info("MEMBER: " + p.toString());
                    DavResourceLocator resourceLocator 
                        = locator.getFactory().createResourceLocator(locator.getPrefix(),
                                                                     locator.getWorkspacePath(),
                                                                     p.toString(),
                                                                     false);
                    try {
                        list.add(factory.createResource(resourceLocator, getSession()));
                    } catch (DavException ex) {
                        ex.printStackTrace();
                    }
                }
            }

        } catch (IOException ex) {
            LOG.warn(StringUtils.stringifyException(ex));
            throw new RuntimeException(ex);
        }
        return new DavResourceIteratorImpl(list);
    }

    public long getModificationTime() {
        try {
            LOG.info("MODIFICATION_TIME: " + fs.getFileStatus(path).getModificationTime());
            return fs.getFileStatus(path).getModificationTime();
        } catch (IOException ex) {
            LOG.warn(StringUtils.stringifyException(ex));
            LOG.info("EXCEPTION: " + StringUtils.stringifyException(ex));
            throw new RuntimeException(ex);
        }
    }


    /*-------------------------------------------------------------------------*/
    /*---------------------------- Property Methods ---------------------------*/
    /*-------------------------------------------------------------------------*/

    private void initProperties() {
        if (inited) {
            return;
        }
        try {
            FileStatus fstat = fs.getFileStatus(getPath());
            properties.add(new DefaultDavProperty(DavPropertyName.GETCONTENTLENGTH, fstat.getLen()));

            SimpleDateFormat simpleFormat =  (SimpleDateFormat) DavConstants.modificationDateFormat.clone();
            simpleFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date date = new Date(fstat.getModificationTime());
            properties.add(new DefaultDavProperty(DavPropertyName.GETLASTMODIFIED, simpleFormat.format(date)));

            properties.add(new DefaultDavProperty(SecurityConstants.OWNER, fstat.getOwner()));
            properties.add(new DefaultDavProperty(SecurityConstants.GROUP, fstat.getGroup()));

            UnixUserGroupInformation ugi = UnixUserGroupInformation.readFromConf(this.conf,
                                                                                 UnixUserGroupInformation.UGI_PROPERTY_NAME);
            CurrentUserPrivilegeSetProperty currentUserPrivilegeSetProperty = UtilsHelper.getCurrentUserPrivilegeSetProperty(fstat, ugi);
            properties.add(new DefaultDavProperty(SecurityConstants.CURRENT_USER_PRIVILEGE_SET,
                                                  currentUserPrivilegeSetProperty.getValue()));
        } catch (IOException ex) {
            LOG.warn(StringUtils.stringifyException(ex));
        } catch (LoginException e) {
            LOG.warn(StringUtils.stringifyException(e));
        }
        // set (or reset) fundamental properties
        if (getDisplayName() != null) {
            properties.add(new DefaultDavProperty(DavPropertyName.DISPLAYNAME, getDisplayName()));
        }
        if (isCollection()) {
            properties.add(new ResourceType(ResourceType.COLLECTION));
            // Windows XP support
            properties.add(new DefaultDavProperty(DavPropertyName.ISCOLLECTION, "1"));
        } else {
            properties.add(new ResourceType(ResourceType.DEFAULT_RESOURCE));
            // Windows XP support
            properties.add(new DefaultDavProperty(DavPropertyName.ISCOLLECTION, "0"));
        }

        //curtently no locking
        /* set current lock information. If no lock is set to this resource,
           an empty lockdiscovery will be returned in the response. */
        //properties.add(new LockDiscovery(getLock(Type.WRITE, Scope.EXCLUSIVE)));

        /* lock support information: all locks are lockable. */
        //SupportedLock supportedLock = new SupportedLock();
        //supportedLock.addEntry(Type.WRITE, Scope.EXCLUSIVE);
        //properties.add(supportedLock);

        inited = true;
    }


    public DavPropertySet getProperties() {
        initProperties();
        return properties;
    }

    public DavProperty getProperty(DavPropertyName name) {
        initProperties();
        return properties.get(name);
    }

    public DavPropertyName[] getPropertyNames() {
        initProperties();
        return properties.getPropertyNames();
    }

    public void removeProperty(DavPropertyName propertyName) throws DavException {
        initProperties();
        properties.remove(propertyName);
    }

    public void setProperty(DavProperty property) throws DavException {
        initProperties();
    }

    public MultiStatusResponse alterProperties(List changeList) throws DavException {
        return null;
    }

    @Deprecated
    public MultiStatusResponse alterProperties(DavPropertySet setProperties,
                                               DavPropertyNameSet removePropertyNames) throws DavException {
        return null;
    }

    //end of property methods

    public String getResourcePath() {
        return locator.getResourcePath();
    }

    public DavSession getSession() {
        return session;
    }

    public String getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    public boolean hasLock(Type type, Scope scope) {
        return false;
    }

    public boolean isCollection() {
        try {
            return fs.getFileStatus(path).isDir();
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isLockable(Type type, Scope scope) {
        return false;
    }

    public ActiveLock lock(LockInfo reqLockInfo) throws DavException {
        return lockManager.createLock(reqLockInfo, this);
    }

    public void unlock(String lockToken) throws DavException {
    }

    public ActiveLock refreshLock(LockInfo reqLockInfo, String lockToken) throws DavException {
        return lockManager.refreshLock(reqLockInfo, lockToken, this);
    }

    public void spool(OutputContext outputContext) throws IOException {
        if (!isCollection()) {
            InputStream input = fs.open(path);
            try {
                IOUtils.copyBytes(input, outputContext.getOutputStream(), conf, false);
            } finally {
                input.close();
            }
        }
    }

}
