package net.mindengine.galen.tests.specs.reader;

import static net.mindengine.galen.specs.Location.locations;
import static net.mindengine.galen.specs.Side.LEFT;
import static net.mindengine.galen.specs.Side.RIGHT;
import static net.mindengine.galen.specs.Side.TOP;
import static net.mindengine.galen.specs.Side.sides;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.util.List;
import java.util.Map;

import net.mindengine.galen.specs.Location;
import net.mindengine.galen.specs.Range;
import net.mindengine.galen.specs.Spec;
import net.mindengine.galen.specs.SpecAbsent;
import net.mindengine.galen.specs.SpecInside;
import net.mindengine.galen.specs.SpecNear;
import net.mindengine.galen.specs.SpecWidth;
import net.mindengine.galen.specs.page.Locator;
import net.mindengine.galen.specs.page.ObjectSpecs;
import net.mindengine.galen.specs.page.PageSection;
import net.mindengine.galen.specs.reader.page.PageSpec;
import net.mindengine.galen.specs.reader.page.PageSpecReader;

import org.testng.annotations.Test;

public class PageSpecsReaderTest {
    
    private static final String BASE_TEST = "shouldLoadSpecSuccessfully";
    PageSpecReader pageSpecReader = new PageSpecReader();
    PageSpec pageSpec;
    
    @Test
    public void shouldBePossible_toReadSpec_fromInputStream() {
        PageSpec pageSpec = new PageSpecReader().read(getClass().getResourceAsStream("/specs.txt"));
        assertThat(pageSpec, is(notNullValue()));
    }
    
    @Test
    public void shouldBePossible_toReadSpec_fromFile() {
        PageSpec pageSpec = new PageSpecReader().read(new File(getClass().getResource("/specs.txt").getFile()));
        assertThat(pageSpec, is(notNullValue()));
    }
    
    @Test
    public void shouldLoadSpecSuccessfully() {
        pageSpec = pageSpecReader.read(new File(getClass().getResource("/specs.txt").getFile()));
    }
    
    @SuppressWarnings("unchecked")
    @Test(dependsOnMethods = BASE_TEST)
    public void shouldRead_objectDefinitions() {
        Map<String, Locator> objects = pageSpec.getObjects();
        assertThat("Amount of objects should be", objects.size(), is(5));
        assertThat(objects, allOf(hasEntry("submit", new Locator("xpath", "//input[@name = 'submit']")), 
                hasEntry("search-field", new Locator("css", "#search")),
                hasEntry("menu", new Locator("id", "menu")),
                hasEntry("big-box", new Locator("tag", "container")),
                hasEntry("anotherObject", new Locator("xpath", "//div"))));
    }
    
    
    @Test(dependsOnMethods = BASE_TEST)
    public void shouldRead_allSpecs_asSections_markedByTags() {
        List<PageSection> sections = pageSpec.getSections();

        assertThat("Amount of sections should be", sections.size(), is(4));
        assertThat(sections.get(0).getTags(), hasSize(0));
        assertThat(sections.get(1).getTags(), allOf(hasSize(2), contains("tablet", "desktop")));
        assertThat(sections.get(2).getTags(), hasSize(0));
        assertThat(sections.get(3).getTags(), allOf(hasSize(1), contains("mobile")));
    }
    
    @Test(dependsOnMethods = BASE_TEST)
    public void shouldRead_allSpecs_withinFirstSection() {
        List<ObjectSpecs> objects = pageSpec.getSections().get(0).getObjects();
        assertThat(objects.size(), is(1));
        
        ObjectSpecs objectSpecs = objects.get(0);
        assertThat(objectSpecs.getObjectName(), is("menu"));
        
        List<Spec> specs = objectSpecs.getSpecs();
        assertThat(specs.size(), is(2));
        
        assertThat((SpecInside) specs.get(0), is(new SpecInside("button", locations(new Location(Range.exact(10), sides(LEFT))))));
        assertThat((SpecWidth) specs.get(1), is(new SpecWidth(Range.exact(70))));
    }

    
    @Test(dependsOnMethods = BASE_TEST)
    public void shouldRead_allSpecs_withinSecondSection() {
        List<ObjectSpecs> objects = pageSpec.getSections().get(1).getObjects();
        assertThat(objects.size(), is(2));
        
        assertThat(objects.get(0).getObjectName(), is("submit"));
        assertThat(objects.get(0).getSpecs().size(), is(2));
        assertThat((SpecInside) objects.get(0).getSpecs().get(0), is(new SpecInside("big-box", 
                locations(
                        new Location(Range.between(10, 30), sides(RIGHT)),
                        new Location(Range.between(20, 40), sides(TOP))
        ))));
        
        assertThat((SpecNear) objects.get(0).getSpecs().get(1), is(new SpecNear("menu", locations(new Location(Range.exact(20), sides(LEFT))))));
        
        assertThat(objects.get(1).getObjectName(), is("search-field"));
        assertThat(objects.get(1).getSpecs().size(), is(1));
        assertThat((SpecInside) objects.get(1).getSpecs().get(0), is(new SpecInside("big-box", locations(new Location(Range.exact(30), sides(RIGHT))))));
    }
    
    @Test(dependsOnMethods = BASE_TEST)
    public void shouldRead_allSpecs_withinThirdSection() {
        List<ObjectSpecs> objects = pageSpec.getSections().get(1).getObjects();
        assertThat(objects.size(), is(1));
        
        ObjectSpecs objectSpecs = objects.get(0);
        assertThat(objectSpecs.getObjectName(), is("big-box"));
        
        List<Spec> specs = objectSpecs.getSpecs();
        assertThat(specs.size(), is(1));
        
        assertThat((SpecWidth) specs.get(0), is(new SpecWidth(Range.exact(900))));
    }
    
    @Test(dependsOnMethods = BASE_TEST)
    public void shouldRead_allSpecs_withinFourthSection() {
        List<ObjectSpecs> objects = pageSpec.getSections().get(1).getObjects();
        assertThat(objects.size(), is(1));
        
        ObjectSpecs objectSpecs = objects.get(0);
        assertThat(objectSpecs.getObjectName(), is("submit"));
        
        List<Spec> specs = objectSpecs.getSpecs();
        assertThat(specs.size(), is(1));
        
        assertThat((SpecAbsent) specs.get(0), is(new SpecAbsent()));
    }
}
