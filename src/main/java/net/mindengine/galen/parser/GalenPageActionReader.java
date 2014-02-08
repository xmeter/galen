/*******************************************************************************
* Copyright 2014 Ivan Shubin http://mindengine.net
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
package net.mindengine.galen.parser;

import static net.mindengine.galen.suite.reader.Line.UNKNOWN_LINE;

import java.awt.Dimension;
import java.util.LinkedList;
import java.util.List;

import net.mindengine.galen.suite.GalenPageAction;
import net.mindengine.galen.suite.actions.GalenPageActionCheck;
import net.mindengine.galen.suite.actions.GalenPageActionCookie;
import net.mindengine.galen.suite.actions.GalenPageActionInjectJavascript;
import net.mindengine.galen.suite.actions.GalenPageActionOpen;
import net.mindengine.galen.suite.actions.GalenPageActionResize;
import net.mindengine.galen.suite.actions.GalenPageActionRunJavascript;
import net.mindengine.galen.utils.GalenUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

public class GalenPageActionReader {

    public static GalenPageAction readFrom(String actionText) {
        String[] args = net.mindengine.galen.parser.CommandLineParser.parseCommandLine(actionText);
        
        if (args.length < 2) {
            throw new SyntaxException(UNKNOWN_LINE, "Cannot parse: " + actionText);
        }
        
        if (args[0].equals("inject")) {
            return injectActionFrom(args);
        }
        else if (args[0].equals("run")) {
            return runActionFrom(args);
        }
        else if (args[0].equals("check")) {
            return checkActionFrom(args, actionText);
        }
        else if (args[0].equals("cookie")) {
            return cookieActionFrom(args);
        }
        else if (args[0].equals("open")) {
            return openActionFrom(args);
        }
        else if (args[0].equals("resize")) {
            return resizeActionFrom(args);
        }
        else throw new SyntaxException(UNKNOWN_LINE, "Unknown action: " + args[0]);
    }

    private static GalenPageAction resizeActionFrom(String[] args) {
        Dimension size = GalenUtils.readSize(args[1]);
        return new GalenPageActionResize(size.width, size.height);
    }

    private static GalenPageAction openActionFrom(String[] args) {
        return new GalenPageActionOpen(args[1]);
    }

    private static GalenPageAction cookieActionFrom(String[] args) {
        GalenPageActionCookie action = new GalenPageActionCookie();
        List<String> cookies = new LinkedList<String>();
        for(int i = 1; i<args.length; i++) {
            cookies.add(args[i]);
        }
        action.setCookies(cookies);
        return action;
    }

    private static GalenPageAction checkActionFrom(String[] args, String originalText) {
        Options options = new Options();
        options.addOption("i", "include", true, "include tags");
        options.addOption("e", "exclude", true, "exclude tags");
        
        org.apache.commons.cli.CommandLineParser parser = new PosixParser();
        
        try {
            CommandLine cmd = parser.parse(options, args);
            String[] leftoverArgs = cmd.getArgs();
         
            if (leftoverArgs == null || leftoverArgs.length < 2) {
                throw new SyntaxException(UNKNOWN_LINE, "There are no page specs: " + originalText);
            }
            
            List<String> specs = new LinkedList<String>();
            for (int i=1; i < leftoverArgs.length; i++) {
                specs.add(leftoverArgs[i]);
            }
            
            return new GalenPageActionCheck()
                .withSpecs(specs)
                .withIncludedTags(readTags(cmd.getOptionValue("i")))
                .withExcludedTags(readTags(cmd.getOptionValue("e")));
        }
        catch (Exception e) {
            throw new SyntaxException(UNKNOWN_LINE, "Couldn't parse: " + originalText, e);
        }
    }

    private static List<String> readTags(String tagsCommaSeparated) {
        if (tagsCommaSeparated != null) {
            String tagsArray[] = tagsCommaSeparated.split(",");
            
            List<String> tags = new LinkedList<String>();
            for (String tag : tagsArray) {
                tag = tag.trim();
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            }
            return tags;
        }
        return null;
    }

    private static GalenPageAction runActionFrom(String[] args) {
        String jsonArguments = null;
        if (args.length > 2) {
            jsonArguments = args[2];
        }
        
        return new GalenPageActionRunJavascript(args[1])
            .withJsonArguments(jsonArguments);
    }

    private static GalenPageActionInjectJavascript injectActionFrom(String[] args) {
        return new GalenPageActionInjectJavascript(args[1]);
    }

}
