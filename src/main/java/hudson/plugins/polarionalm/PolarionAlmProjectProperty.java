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

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;

public class PolarionAlmProjectProperty extends JobProperty<AbstractProject<?,?>> {

	@Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public static final class DescriptorImpl extends JobPropertyDescriptor {
		private String regex;
		private String baseUrl;

		public DescriptorImpl() {
			super(PolarionAlmProjectProperty.class);
			load();
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json)
				throws FormException {
			regex = req.getParameter("polarionalm.regex");
			baseUrl = req.getParameter("polarionalm.base");
			save();
			return true;
		}

		@Override
		public boolean isApplicable(Class<? extends Job> jobType) {
			return false;
		}

		@Override
		public PolarionAlmProjectProperty newInstance(StaplerRequest req,
				JSONObject formData) throws FormException {
			return new PolarionAlmProjectProperty();
		}


		@Override
		public String getDisplayName() {
			return "polarionalm";
		}

		public String getRegex() {
			if(regex == null) return "TASK-[0-9]*";
			return regex;
		}

		public String getBaseUrl() {
			if(baseUrl == null) return "http://localhost/polarion";
			return baseUrl;
		}
	}
}
