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
package net.mindengine.galen.components.validation;

import java.awt.image.BufferedImage;
import java.util.HashMap;

import net.mindengine.galen.page.AbsentPageElement;
import net.mindengine.galen.page.Page;
import net.mindengine.galen.page.PageElement;
import net.mindengine.galen.specs.page.Locator;

public class MockedPage implements Page {

    private HashMap<String, PageElement> elements;
    private BufferedImage screenshotImage;
    
    private HashMap<String, PageElement> locatorElements;

    public MockedPage(HashMap<String, PageElement> elements) {
        this.setElements(elements);
    }
    
    public MockedPage(HashMap<String, PageElement> elements, BufferedImage screenshotImage) {
        this.setElements(elements);
        this.screenshotImage = screenshotImage;
    }
    
    public MockedPage() {
    }

    @Override
    public PageElement getObject(Locator objectLocator) {
        if (locatorElements != null) {
            PageElement pageElement = locatorElements.get(objectLocator.prettyString());
            if (pageElement != null) {
                return pageElement;
            }
        }
        return new AbsentPageElement();
    }

    @Override
    public PageElement getObject(String objectName, Locator locator) {
        return getElements().get(objectName);
    }

    public HashMap<String, PageElement> getElements() {
        return elements;
    }

    public void setElements(HashMap<String, PageElement> elements) {
        this.elements = elements;
    }

    @Override
    public PageElement getSpecialObject(String objectName) {
        return null;
    }

    @Override
    public int getObjectCount(Locator locator) {
        return 0;
    }

    @Override
    public Page createObjectContextPage(Locator mainObjectLocator) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BufferedImage getScreenshotImage() {
        // TODO Auto-generated method stub
        return screenshotImage;
    }

    public void setScreenshotImage(BufferedImage screenshotImage) {
        this.screenshotImage = screenshotImage;
    }

    public HashMap<String, PageElement> getLocatorElements() {
        return locatorElements;
    }

    public void setLocatorElements(HashMap<String, PageElement> locatorElements) {
        this.locatorElements = locatorElements;
    }

}