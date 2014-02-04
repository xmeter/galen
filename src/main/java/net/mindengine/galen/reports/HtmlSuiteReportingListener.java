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
package net.mindengine.galen.reports;

import static java.lang.String.format;

import java.awt.Dimension;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.mindengine.galen.browser.Browser;
import net.mindengine.galen.config.GalenConfig;
import net.mindengine.galen.page.PageElement;
import net.mindengine.galen.reports.model.PageAction;
import net.mindengine.galen.reports.model.PageTest;
import net.mindengine.galen.reports.model.PageTestObject;
import net.mindengine.galen.reports.model.PageTestSection;
import net.mindengine.galen.reports.model.PageTestSpec;
import net.mindengine.galen.runner.CompleteListener;
import net.mindengine.galen.runner.GalenPageRunner;
import net.mindengine.galen.runner.GalenSuiteRunner;
import net.mindengine.galen.specs.Spec;
import net.mindengine.galen.specs.page.PageSection;
import net.mindengine.galen.suite.GalenPageAction;
import net.mindengine.galen.suite.GalenPageTest;
import net.mindengine.galen.suite.GalenSuite;
import net.mindengine.galen.suite.actions.GalenPageActionCheck;
import net.mindengine.galen.validation.PageValidation;
import net.mindengine.galen.validation.ValidationError;

import org.apache.commons.io.FileUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class HtmlSuiteReportingListener implements CompleteListener {
    
    private String reportFolderPath;
    private String reportFileName;
    private Template template;
    private String suiteName = "";
    private List<PageTest> pageTests = new LinkedList<PageTest>();
    private PageTest currentPageTest;
    private PageTestObject currentObject;
    private Browser currentBrowser;
    
    private List<Screenshot> screenshots = new LinkedList<Screenshot>();
    
    //This is needed in order to group objects by name within single page section
    //After each section this map should be cleared
    private Map<String, PageTestObject> cachedPageTestObjectsMap = new HashMap<String, PageTestObject>();
    private Map<String, PageTestSection> cachedSections = new HashMap<String, PageTestSection>();
    
    private int screenshotId = 0;
    private PageAction currentPageAction;
    private PageTestSection currentSection;
    
    private class Screenshot {
        private String name;
        private String filePath;
        public Screenshot(String name, String filePath){
            this.name = name;
            this.filePath = filePath;
        }
    }
    
    

    public HtmlSuiteReportingListener(String reportFolderPath, String reportFileName, Configuration freemarkerConfiguration) throws IOException {
        this.reportFolderPath = reportFolderPath;
        this.reportFileName = reportFileName;
        this.template = new Template("suite-report", new InputStreamReader(getClass().getResourceAsStream("/html-report/report-suite.ftl.html")), freemarkerConfiguration);
    }

    
    @Override
    public void onObject(GalenPageRunner pageRunner, PageValidation pageValidation, String objectName) {
        if (currentObject == null) {
            if (cachedPageTestObjectsMap.containsKey(objectName)) {
                currentObject = cachedPageTestObjectsMap.get(objectName);
            }
            else {
                currentObject = new PageTestObject();
                currentObject.setName(objectName);
                
                PageElement objectPageElement = pageValidation.findPageElement(objectName);
                if (objectPageElement != null && objectPageElement.isVisible()) {
                    currentObject.setArea(objectPageElement.getArea());
                }
                
                currentSection.getObjects().add(currentObject);
                cachedPageTestObjectsMap.put(objectName, currentObject);
            }
        }
        else {
            PageTestObject parentObject = currentObject;
            currentObject = new PageTestObject(parentObject);
            currentObject.setName(objectName);
            
            PageElement objectPageElement = pageValidation.findPageElement(objectName);
            if (objectPageElement != null && objectPageElement.isVisible()) {
                currentObject.setArea(objectPageElement.getArea());
            }
        }
    }

    @Override
    public void onAfterObject(GalenPageRunner pageRunner, PageValidation pageValidation, String objectName) {
        if (currentObject.getParent() != null) {
            currentObject = currentObject.getParent();
        }
        else {
            currentObject = null;
        }
    }

    @Override
    public void onSpecError(GalenPageRunner pageRunner, PageValidation pageValidation, String objectName, Spec originalSpec, ValidationError error) {
        
        PageTestSpec spec = new PageTestSpec();
        currentObject.getSpecs().add(spec);
        
        spec.setText(originalSpec.getOriginalText());
        spec.setFailed(true);
        
        spec.setErrorMessages(error.getMessages());
        spec.setErrorAreas(error.getErrorAreas());
        
        pickSubObjectsForSpec(spec);
    }

    @Override
    public void onSpecSuccess(GalenPageRunner pageRunner, PageValidation pageValidation, String objectName, Spec originalSpec) {
        PageTestSpec spec = new PageTestSpec();
        currentObject.getSpecs().add(spec);
        spec.setText(originalSpec.getOriginalText());
        spec.setFailed(false);
        
        pickSubObjectsForSpec(spec);
    }
    
    private void pickSubObjectsForSpec(PageTestSpec spec) {
        if (currentObject.getSubObjects() != null) {
            spec.setSubObjects(currentObject.getSubObjects());
            currentObject.setSubObjects(null);
        }
    }

    @Override
    public void onGlobalError(GalenPageRunner pageRunner, Exception e) {
        Screenshot screenshot = createScreenShot();
        currentPageTest.getGlobalErrors().add(new PageTest.GlobalError(e, screenshot.name));
    }

    @Override
    public void onAfterPage(GalenSuiteRunner galenSuiteRunner, GalenPageRunner pageRunner, GalenPageTest pageTest, Browser browser, List<ValidationError> errors) {
        if (GalenConfig.getConfig().getBooleanProperty("galen.reporting.html.useLastPageUrls", false)) {
            String url = browser.getUrl();
            Dimension size = browser.getScreenSize();
            currentPageTest.setTitle(format("%s %dx%d", url, size.width, size.height));
        }
    }

    @Override
    public void onBeforePage(GalenSuiteRunner galenSuiteRunner, GalenPageRunner pageRunner, GalenPageTest pageTest, Browser browser) {
        currentBrowser = browser;
        
        currentPageTest = new PageTest();
        
        if (pageTest.getTitle() != null) {
            currentPageTest.setTitle(pageTest.getTitle());
        }
        else {
            String title = pageTest.getUrl();
            if (pageTest.getScreenSize() != null) {
                title = title + " " + pageTest.getScreenSize().width + "x" + pageTest.getScreenSize().height;
            }
            currentPageTest.setTitle(title);
        }
        pageTests.add(currentPageTest);
    }

    @Override
    public void onSuiteFinished(GalenSuiteRunner galenSuiteRunner, GalenSuite suite) {
    }

    @Override
    public void onSuiteStarted(GalenSuiteRunner galenSuiteRunner, GalenSuite suite) {
        this.suiteName = suite.getName();
    }

    @Override
    public void done() {
        try {
            HtmlReportingListener.makeSureReportFolderExists(reportFolderPath);
            
            File file = new File(reportFolderPath + File.separator + reportFileName + ".html");
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new RuntimeException("Cannot create file: " + file.getAbsolutePath());
                }
            }
            FileWriter fileWriter = new FileWriter(file);
            Map<String, Object> model = new HashMap<String, Object>();
            model.put("suiteName", suiteName);
            model.put("pageTests", pageTests);
            template.process(model, fileWriter);
            fileWriter.flush();
            fileWriter.close();
            
            moveScreenshots();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    


    private void moveScreenshots() throws IOException {
        File folder = new File(reportFolderPath);
        for (Screenshot screenshot : screenshots) {
            FileUtils.copyFile(new File(screenshot.filePath), new File(folder.getAbsolutePath() + File.separator + screenshot.name));
        }
    }


    private synchronized Screenshot createScreenShot() {
        screenshotId++;
        String filePath = currentBrowser.createScreenshot();
        Screenshot screenshot = new Screenshot(reportFileName + "-screenshot-" + screenshotId + "." + extensionFrom(filePath), filePath);
        screenshots.add(screenshot);
        return screenshot;
    }

    private String extensionFrom(String filePath) {
        StringBuilder extension = new StringBuilder();
        for (int i = filePath.length() - 1; i >= 0; i--) {
            char ch = filePath.charAt(i);
            if (ch == '.') {
                break;
            }
            else extension.append(ch);
        }
        return extension.reverse().toString();
    }


    @Override
    public void onBeforePageAction(GalenPageRunner pageRunner, GalenPageAction action) {
        currentPageAction = new PageAction();
        
        //Reseting cache for sections
        cachedSections = new HashMap<String, PageTestSection>();
        
        currentPageAction.setTitle(action.getOriginalCommand());
        
        if (action instanceof GalenPageActionCheck) {
            String screenshotPath = createScreenShot().name;
            currentPageAction.setScreenshot(screenshotPath);
        }
        
        
        currentPageTest.getPageActions().add(currentPageAction);
    }
    
    @Override
    public void onAfterPageAction(GalenPageRunner pageRunner, GalenPageAction action) {
    }


    /* Using section level counter so that in html reports only the higher level section gets shown 
     * */ 
    int sectionLevel = 0;
    
    @Override
    public void onBeforeSection(GalenPageRunner pageRunner, PageValidation pageValidation, PageSection pageSection) {
        sectionLevel++;
        
        if (sectionLevel <= 1) {
            //Reseting objects map
            cachedPageTestObjectsMap = new HashMap<String, PageTestObject>();
            
            String name = pageSection.getName();
            if (name == null || name.trim().isEmpty()) {
                name = "Unnamed";
            }
            
            currentSection = createSection(name);
        }
    }


    private PageTestSection createSection(String name) {
        if (cachedSections.containsKey(name)) {
            return cachedSections.get(name);
        }
        else {
            PageTestSection section = new PageTestSection();
            section.setName(name);
            cachedSections.put(name, section);
            currentPageAction.getSections().add(section);
            return section;
        }
    }


    @Override
    public void onAfterSection(GalenPageRunner pageRunner, PageValidation pageValidation, PageSection pageSection) {
        sectionLevel--;
    }
}
