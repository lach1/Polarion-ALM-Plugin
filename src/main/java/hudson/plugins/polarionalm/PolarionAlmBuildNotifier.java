/* 
 * Copyright (c) 2012 Software Competence Center Hagenber GmbH (www.scch.at)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * The original copy of this license is available at 
 * http://www.opensource.org/licenses/MIT
 */
 
package hudson.plugins.polarionalm;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.rpc.ServiceException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.polarion.alm.ws.client.WebServiceFactory;
import com.polarion.alm.ws.client.projects.ProjectWebService;
import com.polarion.alm.ws.client.session.SessionWebService;
import com.polarion.alm.ws.client.tracker.TrackerWebService;
import com.polarion.alm.ws.client.types.Text;
import com.polarion.alm.ws.client.types.projects.Project;
import com.polarion.alm.ws.client.types.tracker.EnumOptionId;
import com.polarion.alm.ws.client.types.tracker.WorkItem;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

public class PolarionAlmBuildNotifier extends Notifier {

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private static final Logger logger = Logger.getLogger(PolarionAlmBuildNotifier.class.getName());

    private WebServiceFactory factory;
    private TrackerWebService trackerService;
    private SessionWebService sessionService;
    private ProjectWebService projectService;

    private String workitemTitle;
    private String workitemType;
    private String project;
    private String workitemCategory;
    private String linkedWorkitemId;
    private String linkedWorkitemProject;
	private String linkedWorkitemRole;

	private String hudsonBaseUrl;

    public PolarionAlmBuildNotifier() {
        super();
    }

    @DataBoundConstructor
    public PolarionAlmBuildNotifier(String project, String workitemTitle, String workitemType, String workitemCategory, String linkedWorkitemId, String linkedWorkitemProject, String linkedWorkitemRole) {
        this.project = project;
        this.workitemTitle = workitemTitle;
        this.workitemType = workitemType;
        this.workitemCategory = workitemCategory;
        this.linkedWorkitemId = linkedWorkitemId;
        this.linkedWorkitemProject = linkedWorkitemProject;
        this.linkedWorkitemRole = linkedWorkitemRole;
        this.hudsonBaseUrl = Hudson.getInstance().getRootUrl();
    }

    public String getWorkitemTitle() {
        return workitemTitle;
    }

    public String getWorkitemType() {
        return workitemType;
    }

    public String getProject() {
        return project;
    }

    public String getWorkitemCategory() {
        return workitemCategory;
    }

    public String getLinkedWorkitemId() {
    	return linkedWorkitemId;
    }

    public String getLinkedWorkitemProject() {
    	return linkedWorkitemProject;
    }

    public String getLinkedWorkitemRole() {
    	return linkedWorkitemRole;
    }

    private void initializePolarionConnection() {
        if (DESCRIPTOR.getBaseUrl() != null) {
            String polarionUrl = DESCRIPTOR.getBaseUrl()+"/polarion/ws/services/";
            logger.log(Level.FINE, "Using Polarion URL "+polarionUrl);
            try {
                factory = new WebServiceFactory(polarionUrl);
                trackerService = factory.getTrackerService();
                sessionService = factory.getSessionService();
                projectService = factory.getProjectService();
                logger.log(Level.FINE, "Logging in to Polarion "+polarionUrl+" with user "+DESCRIPTOR.getUsername());
                sessionService.logIn(DESCRIPTOR.getUsername(), DESCRIPTOR.getPassword());
            } catch (MalformedURLException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage());
            } catch (ServiceException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage());
            } catch (RemoteException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage());
            }
        }
        else {
            logger.log(Level.WARNING, "Polarion base Url not configured");
        }
    }

    private void destroyPolarionConnection() {
        try {
            if (sessionService != null)
                sessionService.endSession();
            sessionService = null;
            trackerService = null;
            projectService = null;
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage());
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public BuildStepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        Result result = build.getResult();
        if (result != Result.SUCCESS) {
            if (checkWorkitemConfiguration()) {
                logger.log(Level.INFO, "Build failed - creating workitem "+workitemType+" in projet "+project);
                createPolarionDefect(build);
            }
        }
        return true;
    }

    private void createPolarionDefect(AbstractBuild<?, ?> build) {
        try {
            initializePolarionConnection();
            createWorkitem(build);
            destroyPolarionConnection();
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage());
        }
    }

    private boolean checkWorkitemConfiguration() {
        if (DESCRIPTOR.getBaseUrl() == null || DESCRIPTOR.getBaseUrl().length() == 0) {
            logger.log(Level.WARNING, "Polarion base url not configured");
            return false;
        }
        if (DESCRIPTOR.getUsername() == null || DESCRIPTOR.getUsername().length() == 0) {
            logger.log(Level.WARNING, "Polarion username not configured");
            return false;
        }
        if (DESCRIPTOR.getPassword() == null || DESCRIPTOR.getPassword().length() == 0) {
        	logger.log(Level.WARNING, "Polarion password not configured");
        	return false;
        }
        if (project == null || project.length() == 0) {
            logger.log(Level.WARNING, "Polarion project not configured");
            return false;
        }
        if (workitemTitle == null || workitemTitle.length() == 0) {
            logger.log(Level.WARNING, "Workitem title not configured");
            return false;
        }
        if (workitemCategory == null || workitemCategory.length() == 0) {
            logger.log(Level.WARNING, "Workitem category not configured");
            return false;
        }
        if (workitemType == null || workitemType.length() == 0) {
            logger.log(Level.WARNING, "Workitem type not configured");
            return false;
        }
        // check linked workitem role and project only when linkedworkitemid is set
        if (linkedWorkitemId != null && linkedWorkitemRole == null) {
        	logger.log(Level.WARNING, "Linked workitem role not configured");
        }
        if (linkedWorkitemId != null && linkedWorkitemProject == null) {
        	logger.log(Level.WARNING, "Linked workitem project not configured");
        }
        return true;
    }

    private void createWorkitem(AbstractBuild<?, ?> build) throws RemoteException {
        if (sessionService != null) {
            sessionService.beginTransaction();

            WorkItem wi = new WorkItem();
            // set title
            String workitemTitleParsed = workitemTitle;
            if (workitemTitleParsed.contains("%b")) {
                workitemTitleParsed = workitemTitleParsed.replace("%b", Integer.toString(build.getNumber()));
            }
            if (workitemTitleParsed.contains("%n")) {
            	workitemTitleParsed = workitemTitleParsed.replace("%n", build.getProject().getName());
            }
            wi.setTitle(workitemTitleParsed);
            // set description
            try {
                Text descriptionText;
                StringBuffer description = new StringBuffer();
                for (String descriptionLine : build.getLog(DESCRIPTOR.getBuildlog())) {
                    description.append(descriptionLine);
                    description.append('\n');
                }
                descriptionText = new Text("text/plain", description.toString(), false);
                wi.setDescription(descriptionText);
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getLocalizedMessage());
            }
            // set type
            EnumOptionId type = new EnumOptionId(workitemType);
            wi.setType(type);
            // set project
            Project	polarionProject = projectService.getProject(project);
            wi.setProject(polarionProject);

            String wiURI = trackerService.createWorkItem(wi);

            // set category
            trackerService.addCategory(wiURI, workitemCategory);
            // set assignee
            Set<User> culprits = build.getCulprits();
            for (User culprit : culprits) {
                trackerService.addAssignee(wiURI, culprit.getId());
            }
            // set Url to build
            trackerService.addHyperlink(wiURI, hudsonBaseUrl+'/'+build.getUrl(), new EnumOptionId("ref_ext"));
            // add linked workitem with specific role
            if (linkedWorkitemId != null && linkedWorkitemRole != null) {
	            WorkItem linkedWi = trackerService.getWorkItemById(linkedWorkitemProject, linkedWorkitemId);
	            trackerService.addLinkedItem(wiURI, linkedWi.getUri(), new EnumOptionId(linkedWorkitemRole));
            }

            sessionService.endTransaction(false);
            sessionService.endSession();
        }
    }

    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String polarionUrl;
        private String username;
        private String password;
        private int buildlog;

        public DescriptorImpl() {
            super(PolarionAlmBuildNotifier.class);
            load();
        }

        private boolean checkNonEmptyValue(String value) {
            return value != null && value.length() > 0;
        }

        public FormValidation doCheckProject(@QueryParameter final String value) {
            if (!checkNonEmptyValue(value))
                return FormValidation.error("Project missing");
            return FormValidation.ok();
        }

        public FormValidation doCheckWorkitemType(@QueryParameter final String value) {
            if (!checkNonEmptyValue(value))
                return FormValidation.error("Workitem type missing");
            return FormValidation.ok();
        }

        public FormValidation doCheckWorkitemCategory(@QueryParameter final String value) {
            if (!checkNonEmptyValue(value))
                return FormValidation.error("Workitem category missing");
            return FormValidation.ok();
        }

        public FormValidation doCheckWorkitemTitle(@QueryParameter final String value) {
            if (!checkNonEmptyValue(value))
                return FormValidation.error("Workitem title missing");
            return FormValidation.ok();
        }

        public String getBaseUrl() {
            return polarionUrl;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public int getBuildlog() {
            return buildlog;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Polarion ALM Build Notifier";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            polarionUrl = req.getParameter("polarionalmbuild.base");
            username = req.getParameter("polarionalmbuild.username");
            password = req.getParameter("polarionalmbuild.password");
            buildlog = Integer.parseInt(req.getParameter("polarionalmbuild.buildlog"));
            save();
            return super.configure(req, json);
        }
    }
}
