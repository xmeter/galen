/*******************************************************************************
* Copyright 2013 Ivan Shubin http://mindengine.net
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************************/
package net.mindengine.galen.specs.reader.page;

import static net.mindengine.galen.suite.reader.Line.UNKNOWN_LINE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import net.mindengine.galen.parser.ExpectWord;
import net.mindengine.galen.parser.SyntaxException;
import net.mindengine.galen.specs.page.PageSection;


public class PageSpecLineProcessor {

    private static final String TAG = "@";
    private static final String COMMENT = "#";
    private static final String PARAMETERIZATION_SYMBOL = "[";
    PageSpec pageSpec = new PageSpec();
    
    private State state;
	private PageSpecReader pageSpecReader;
    
    public PageSpecLineProcessor(PageSpecReader pageSpecReader) {
    	this.pageSpecReader = pageSpecReader;
        startNewSection("");
    }

    public void processLine(String line) throws IOException {
        if (!isCommentedOut(line) && !isEmpty(line)) {
        	if (isSpecialInstruction(line)) {
        		doSpecialInstruction(line);
        	}
        	else if (isObjectSeparator(line)) {
                switchObjectDefinitionState();
            }
            else if (line.startsWith(TAG)) {
                startNewSection(line.substring(1));
            }
            else if (isSectionSeparator(line)) {
                //Do nothing
            }
            else if (line.startsWith(PARAMETERIZATION_SYMBOL)) {
                startParameterization(line);
            }
            else state.process(line);
        }
    }
    
    private void doSpecialInstruction(String line) throws IOException {
		line = line.trim().substring(2).trim();
		
		String firstWord = ExpectWord.read(line);
		
		if (firstWord.equals("import")) {
			importPageSpec(line.substring(6).trim());
		}
	}

	private void importPageSpec(String filePath) throws IOException {
		PageSpec spec = pageSpecReader.read(new File(filePath));
		if (spec != null) {
			pageSpec.merge(spec);
		}
	}

	private boolean isSpecialInstruction(String line) {
    	return line.trim().startsWith("@@");
	}

	private void startParameterization(String line) {
        line = line.replace(" ", "");
        line = line.replace("\t", "");
        Pattern sequencePattern = Pattern.compile("[0-9]+\\-[0-9]+");
        try {
            line = line.substring(1, line.length() - 1);
            String[] values = line.split(",");
            
            ArrayList<String> parameterization = new ArrayList<String>();
            
            for (String value : values) {
                if (sequencePattern.matcher(value).matches()) {
                    parameterization.addAll(createSequence(value));
                }
                else {
                    parameterization.add(value);
                }
            }
            
            startParameterization(parameterization.toArray(new String[]{}));
        }
        catch (Exception ex) {
            throw new SyntaxException(UNKNOWN_LINE, "Incorrect parameterization syntax", ex);
        }
        
    }

    private List<String> createSequence(String value) {
        int dashIndex = value.indexOf('-');
        
        int rangeA = Integer.parseInt(value.substring(0, dashIndex));
        int rangeB = Integer.parseInt(value.substring(dashIndex + 1));
        
        int min = Math.min(rangeA, rangeB);
        int max = Math.max(rangeA, rangeB);
        return createSequence(min, max);
    }

    private List<String> createSequence(int min, int max) {
        List<String> parameters = new LinkedList<String>();
        for (int i = min; i <= max; i++) {
            parameters.add(Integer.toString(i));
        }
        return parameters;
    }

    private void startParameterization(String[] parameters) {
        if (!(state instanceof StateDoingSection)) {
            startNewSection("");
        }
        
        StateDoingSection doingSection = (StateDoingSection)state;
        doingSection.parameterizeNextObject(parameters);
    }

    public PageSpec buildPageSpec() {
        Iterator<PageSection> it = pageSpec.getSections().iterator();
        while(it.hasNext()) {
            if (it.next().getObjects().size() == 0) {
                it.remove();
            }
         }
        return this.pageSpec;
    }

    private void switchObjectDefinitionState() {
        if (state.isObjectDefinition()) {
            startNewSection("");
        }
        else state = State.objectDefinition(pageSpec);
    }

    private boolean isSectionSeparator(String line) {
        line = line.trim();
        if (line.length() < 4) {
            return false;
        }
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != '-') {
                return false;
            }
        }
        
        return true;
    }

    private boolean isObjectSeparator(String line) {
        return containsOnly(line.trim(), '=');
    }

    private void startNewSection(String tags) {
        PageSection section = new PageSection();
        section.setTags(readTags(tags));
        pageSpec.addSection(section);
        state = State.startedSection(section);
    }

    private List<String> readTags(String tagsText) {
        List<String> tags = new LinkedList<String>();
        for (String tag : tagsText.split(",")) {
            tag = tag.trim();
            if(!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private boolean isEmpty(String line) {
        return line.trim().isEmpty();
    }

    private boolean isCommentedOut(String line) {
        return line.trim().startsWith(COMMENT);
    }

    private boolean containsOnly(String line, char c) {
        if (line.length() > 1) {
            for (int i=0; i<line.length(); i++) {
                if (line.charAt(i) != c) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
