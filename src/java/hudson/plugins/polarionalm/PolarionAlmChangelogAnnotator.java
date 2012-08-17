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

import hudson.Extension;
import hudson.MarkupText;
import hudson.MarkupText.SubText;
import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogAnnotator;
import hudson.scm.ChangeLogSet.Entry;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Extension
public class PolarionAlmChangelogAnnotator extends ChangeLogAnnotator {
	
	private static final Logger LOGGER = Logger.getLogger(PolarionAlmChangelogAnnotator.class.getName());
	
	private static String getId(SubText token) {
		String id = null;
		for(int i = 0;;i++) {
			id = token.group(i);
			return id;
		}
	}
	
	@Override
	public void annotate(AbstractBuild<?, ?> build, Entry change,
			MarkupText text) {
		Pattern pattern = null;
		String regex = PolarionAlmProjectProperty.DESCRIPTOR.getRegex();
		try {
			pattern = Pattern.compile(regex);
		} catch (PatternSyntaxException e) {
			LOGGER.log(Level.WARNING, "Cannot compile pattern: {0}", regex);
			return;
		}

		for(SubText token : text.findTokens(pattern)) {
			String key = null;
			try {
				key = getId(token);
			} catch (Exception e) {
				continue;
			}
			String baseUrl = PolarionAlmProjectProperty.DESCRIPTOR.getBaseUrl();
			token.surroundWith(String.format("<a href='%s/#/workitem?id=%s'>", baseUrl, key), "</a>");
		}
	}
}
