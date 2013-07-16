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
package net.mindengine.galen.validation.specs;

import static java.lang.String.format;
import net.mindengine.galen.page.PageElement;
import net.mindengine.galen.specs.SpecAbsent;
import net.mindengine.galen.validation.PageValidation;
import net.mindengine.galen.validation.SpecValidation;
import net.mindengine.galen.validation.ValidationError;

public class SpecValidationAbsent extends SpecValidation<SpecAbsent>{

    public SpecValidationAbsent(PageValidation pageValidation) {
        super(pageValidation);
    }

    @Override
    public ValidationError check(String objectName, SpecAbsent spec) {
        PageElement mainObject = getPageElement(objectName);
        if (mainObject == null) {
            return errorObjectMissingInSpec(objectName);
        }
        else if (mainObject.isPresent() && mainObject.isVisible()) {
            return new ValidationError(mainObject.getArea(), format("Object \"%s\" is not absent on page", objectName));
        }
        else return null;
    }

}
