package net.mindengine.galen.tests.specs.reader;

import static net.mindengine.galen.specs.Side.BOTTOM;
import static net.mindengine.galen.specs.Side.LEFT;
import static net.mindengine.galen.specs.Side.RIGHT;
import static net.mindengine.galen.specs.Side.TOP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.List;

import net.mindengine.galen.specs.Location;
import net.mindengine.galen.specs.Range;
import net.mindengine.galen.specs.Side;
import net.mindengine.galen.specs.reader.ExpectLocations;
import net.mindengine.galen.specs.reader.ExpectRange;
import net.mindengine.galen.specs.reader.ExpectSides;
import net.mindengine.galen.specs.reader.ExpectWord;
import net.mindengine.galen.specs.reader.StringCharReader;

import org.apache.commons.lang3.StringEscapeUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ExpectationsTest {

    @Test(dataProvider = "rangeTestData")
    public void expectRangeTest(TestData<Range> testData) {
        StringCharReader stringCharReader = new StringCharReader(testData.textForParsing);
        Range range = new ExpectRange().read(stringCharReader);
        assertThat(range.getFrom(), is(testData.expected.getFrom()));
        assertThat(range.getTo(), is(testData.expected.getTo()));
    }
    
    @DataProvider
    public Object[][] rangeTestData() {
        return new Object[][]{
           row("10 to 15 px", new Range(10, 15)),
           row("10 to 15px", new Range(10, 15)),
           row("10to15px", new Range(10, 15)),
           row("-10to-15px", new Range(-15, -10)),
           row("10to15 px", new Range(10, 15)),
           row("9 px", new Range(9, null)),
           row("9px", new Range(9, null)),
           row("   9px", new Range(9, null)),
           row("\t9px", new Range(9, null)),
           row("\t9\t\tpx", new Range(9, null)),
           row("-49px", new Range(-49, null)),
           row("15 � 5 px", new Range(10, 20)),
           row("15�5px", new Range(10, 20)),
           row("15�5px", new Range(10, 20))
        };
    }
    
    
    @Test(dataProvider = "wordTestData")
    public void expectWord(TestData<String> testData) {
        StringCharReader stringCharReader = new StringCharReader(testData.textForParsing);
        String word = new ExpectWord().read(stringCharReader);
        assertThat(word, is(testData.expected));
    }
    
    @DataProvider
    public Object[][] wordTestData() {
        return new Object[][]{
           row("object", "object"),
           row("  object", "object"),
           row("\tobject ", "object"),
           row("\t\tobject\tanother", "object"),
           row("o ject", "o"),
           row("   je ct", "je")
        };
    }
    
    @Test(dataProvider = "sideTestData")
    public void expectSides(TestData<List<Side>> testData) {
        StringCharReader stringCharReader = new StringCharReader(testData.textForParsing);
        List<Side> sides = new ExpectSides().read(stringCharReader);
        
        Side[] expected = testData.expected.toArray(new Side[testData.expected.size()]);
        assertThat(sides.size(), is(expected.length));
        assertThat(sides, contains(expected));
    }
    
    @DataProvider
    public Object[][] sideTestData() {
        return new Object[][]{
           row("left right", sides(LEFT, RIGHT)),
           row("    \tleft\t  right  ", sides(LEFT, RIGHT)),
           row("   left   ", sides(LEFT)),
           row("top  left   ", sides(TOP, LEFT)),
           row("top  left  bottom ", sides(TOP, LEFT, BOTTOM))
        };
    }
    
    @Test(dataProvider = "locationsTestData")
    public void expectLocations(TestData<List<Location>> testData) {
        StringCharReader stringCharReader = new StringCharReader(testData.textForParsing);
        List<Location> sides = new ExpectLocations().read(stringCharReader);
        
        Location[] expected = testData.expected.toArray(new Location[testData.expected.size()]);
        assertThat(sides.size(), is(expected.length));
        assertThat(sides, contains(expected));
    }
    
    @DataProvider
    public Object[][] locationsTestData() {
        return new Object[][]{
           row("10 px left right, 10 to 20 px top bottom", locations(new Location(Range.exact(10), sides(LEFT, RIGHT)), new Location(Range.between(10, 20), sides(TOP, BOTTOM)))),
           row("10 px left, 10 to 20 px top bottom, 30px right", locations(new Location(Range.exact(10), sides(LEFT)), 
                   new Location(Range.between(10, 20), sides(TOP, BOTTOM)), 
                   new Location(Range.exact(30), sides(RIGHT)))),
           row("   10 px left right   ,   10 to 20 px top bottom  ", locations(new Location(Range.exact(10), sides(LEFT, RIGHT)), new Location(Range.between(10, 20), sides(TOP, BOTTOM)))),
           row("\t10 px left right\t,\t10 to 20 px\ttop\tbottom \t \t \t", locations(new Location(Range.exact(10), sides(LEFT, RIGHT)), new Location(Range.between(10, 20), sides(TOP, BOTTOM)))),
        };
    }
    
    
    private List<Location> locations(Location...locations) {
        return Arrays.asList(locations);
    }

    private List<Side> sides(Side...sides) {
        return Arrays.asList(sides);
    }

    private <T> Object[] row(String textForParsing, T expectedRange) {
        return new Object[]{new TestData<T>(textForParsing, expectedRange)};
    }


    private class TestData<T> {
        private String textForParsing;
        private T expected;
        
        public TestData(String textForParsing, T expected) {
            this.textForParsing = textForParsing;
            this.expected = expected;
        }
        
        @Override
        public String toString() {
            return StringEscapeUtils.escapeJava(textForParsing);
        }
    }
}
