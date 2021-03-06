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
package net.mindengine.galen.validation;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import net.mindengine.galen.runner.GalenPageRunner;
import net.mindengine.galen.specs.Spec;
import net.mindengine.galen.specs.page.ConditionalBlock;
import net.mindengine.galen.specs.page.ConditionalBlockStatement;
import net.mindengine.galen.specs.page.ObjectSpecs;
import net.mindengine.galen.specs.page.PageSection;

public class SectionValidation {

    private static final GalenPageRunner UNKNOWN_PAGE_RUNNER = null;
    private static final List<ValidationError> EMPTY_ERRORS = new LinkedList<ValidationError>();
    private static final boolean SHOULD_REPORT = true;
    private static final boolean SHOULD_NOT_REPORT = false;
    private List<PageSection> pageSections;
    private PageValidation pageValidation;
    private ValidationListener validationListener;

    public SectionValidation(List<PageSection> pageSections, PageValidation pageValidation, ValidationListener validationListener) {
        this.pageSections = pageSections;
        this.pageValidation = pageValidation;
        this.validationListener = validationListener;
    }

    public List<ValidationError> check() {
        
        //Fetching all multi objects from page before validation
        pageValidation.getPageSpec().updateMultiObjects(pageValidation.getPage());
        
        List<ValidationError> errors = new LinkedList<ValidationError>();
        
        for (PageSection section : pageSections) {
            errors.addAll(checkSection(section));
        }
        return errors;
    }

    private List<ValidationError> checkSection(PageSection section) {
        tellBeforeSection(section);
        List<ValidationError> errors = new LinkedList<ValidationError>();
        errors.addAll(checkObjects(section.getObjects()));
        
        List<ConditionalBlock> conditionalBlocks = section.getConditionalBlocks();
        if (conditionalBlocks != null) {
            for (ConditionalBlock block : conditionalBlocks) {
                errors.addAll(checkConditionalBlock(block));
            }
        }
        
        tellAfterSection(section);
        return errors;
    }

    private void tellAfterSection(PageSection section) {
        if (validationListener != null) {
            validationListener.onAfterSection(UNKNOWN_PAGE_RUNNER, pageValidation, section);
        }
    }

    private void tellBeforeSection(PageSection section) {
        if (validationListener != null) {
            validationListener.onBeforeSection(UNKNOWN_PAGE_RUNNER, pageValidation, section);
        }
    }

    private List<ValidationError> checkObjects(List<ObjectSpecs> objects, boolean shouldReport) {
        List<ValidationError> errors = new LinkedList<ValidationError>();
        for (ObjectSpecs object : objects) {
            List<String> allObjectNames = findAllObjectNames(object.getObjectName());
            for (String objectName : allObjectNames) {
                if (shouldReport) {
                    tellOnObject(objectName);
                }
                
                errors.addAll(checkObject(objectName, object.getSpecs(), shouldReport));
                
                if (shouldReport) {
                    tellOnAfterObject(objectName);
                }
            }
        }
        return errors;
    }

    private List<ValidationError> checkConditionalBlock(ConditionalBlock block) {
        if (oneOfConditionsApplies(block.getStatements())) {
            return checkObjects(block.getBodyObjects());
        }
        else if (block.getOtherwiseObjects() != null) {
            return checkObjects(block.getOtherwiseObjects());
        }
        else return EMPTY_ERRORS;
    }

    private List<ValidationError> checkObjects(List<ObjectSpecs> bodyObjects) {
        return checkObjects(bodyObjects, SHOULD_REPORT);
    }

    private boolean oneOfConditionsApplies(List<ConditionalBlockStatement> statements) {
        for (ConditionalBlockStatement statement : statements) {
            List<ValidationError> errors = checkObjectsSilently(statement.getObjects());
            
            boolean statementStatus =  (errors == null || errors.size() == 0);
            if (statement.isInverted()) {
                statementStatus = !statementStatus;
            }
            
            if (statementStatus) {
                return true;
            }
        }
        return false;
    }

    private List<ValidationError> checkObjectsSilently(List<ObjectSpecs> objects) {
        return checkObjects(objects, SHOULD_NOT_REPORT);
    }

    
    private List<String> findAllObjectNames(String objectsDefinition) {
        List<String> objectNames = new LinkedList<String>();
        
        String names[] = objectsDefinition.split(",");
        
        for (String name : names) {
            name = name.trim();
            if (!name.isEmpty()) {
                if (isRegularExpression(name)) {
                    objectNames.addAll(fetchUsingRegex(name));
                }
                else {
                    objectNames.add(name);
                }
            }
        }
        return objectNames;
    }

    private List<String> fetchUsingRegex(String simpleRegex) {
        String regex = simpleRegex.replace("*", "[a-zA-Z0-9_]+");
        Pattern pattern = Pattern.compile(regex);
        
        List<String> objectNames = new LinkedList<String>();
        for (String objectName : pageValidation.getPageSpec().getObjects().keySet()) {
            if (pattern.matcher(objectName).matches()) {
                objectNames.add(objectName);
            }
        }
        
        return objectNames;
    }

    private boolean isRegularExpression(String name) {
        if (name.contains("*")) {
            return true;
        }
        else return false;
    }

    private void tellOnAfterObject(String objectName) {
        if (validationListener != null) {
            try {
                validationListener.onAfterObject(UNKNOWN_PAGE_RUNNER, pageValidation, objectName);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        } 
    }

    private void tellOnObject(String objectName) {
        if (validationListener != null) {
            try {
                validationListener.onObject(UNKNOWN_PAGE_RUNNER, pageValidation, objectName);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private List<ValidationError> checkObject(String objectName, List<Spec> specs, boolean shouldReport) {
        List<ValidationError> errors = new LinkedList<ValidationError>();
        for (Spec spec : specs) {
            
            ValidationError error = pageValidation.check(objectName, spec);
            if (error != null) {
                errors.add(error);
                if (shouldReport) {
                    tellOnSpecError(pageValidation, objectName, spec, error);
                }
            }
            else if (shouldReport) {
                tellOnSpecSuccess(pageValidation, objectName, spec);
            }
        }
        return errors;
    }

    private void tellOnSpecError(PageValidation pageValidation, String objectName, Spec spec, ValidationError error) {
        try {
            if (validationListener != null) {
                validationListener.onSpecError(UNKNOWN_PAGE_RUNNER, pageValidation, objectName, spec, error);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tellOnSpecSuccess(PageValidation pageValidation, String objectName, Spec spec) {
        try {
            if (validationListener != null) {
                validationListener.onSpecSuccess(UNKNOWN_PAGE_RUNNER, pageValidation, objectName, spec);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
