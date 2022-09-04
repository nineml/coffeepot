package org.nineml.coffeepot;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.nineml.coffeepot.utils.RecordSplitter;

import java.util.List;

public class RecordSplitterTest {

    @Test
    public void newlineSplit0() {
        List<String> result = RecordSplitter.splitOnEnd("abc", "\\n");
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("abc", result.get(0));
    }

    @Test
    public void newlineSplit1() {
        List<String> result = RecordSplitter.splitOnEnd("a\nb\nc\n", "\\n");
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("b", result.get(1));
        Assertions.assertEquals("c", result.get(2));
    }

    @Test
    public void newlineSplit2() {
        List<String> result = RecordSplitter.splitOnEnd("a\nb\nc", "\\n");
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("b", result.get(1));
        Assertions.assertEquals("c", result.get(2));
    }

    @Test
    public void newlineSplit3() {
        List<String> result = RecordSplitter.splitOnEnd("a\nb\nc\n", "(\\n)");
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a\n", result.get(0));
        Assertions.assertEquals("b\n", result.get(1));
        Assertions.assertEquals("c\n", result.get(2));
    }

    @Test
    public void newlineSplit4() {
        List<String> result = RecordSplitter.splitOnEnd("a\nb\nc", "(\\n)");
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a\n", result.get(0));
        Assertions.assertEquals("b\n", result.get(1));
        Assertions.assertEquals("c", result.get(2));
    }

    @Test
    public void digitSplitEnd_nocapture1() {
        List<String> result = RecordSplitter.splitOnEnd("aX1234bX5678cX90", "X[0-9]+");
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("b", result.get(1));
        Assertions.assertEquals("c", result.get(2));
    }

    @Test
    public void digitSplitEnd_capturex1() {
        List<String> result = RecordSplitter.splitOnEnd("aX1234bX5678cX90", "(X)[0-9]+");
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("aX", result.get(0));
        Assertions.assertEquals("bX", result.get(1));
        Assertions.assertEquals("cX", result.get(2));
    }

    @Test
    public void digitSplitEnd_nocapture2() {
        List<String> result = RecordSplitter.splitOnEnd("aX1234bX5678cX90d", "X[0-9]+");
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("b", result.get(1));
        Assertions.assertEquals("c", result.get(2));
        Assertions.assertEquals("d", result.get(3));
    }

    @Test
    public void digitSplitEnd_capturex2() {
        List<String> result = RecordSplitter.splitOnEnd("aX1234bX5678cX90d", "(X)[0-9]+");
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("aX", result.get(0));
        Assertions.assertEquals("bX", result.get(1));
        Assertions.assertEquals("cX", result.get(2));
        Assertions.assertEquals("d", result.get(3));
    }

    @Test
    public void digitSplitEnd_capture_multiple1() {
        List<String> result = RecordSplitter.splitOnEnd("aX1234YbX5678YcX90Yd", "(X)[0-9]+(Y)");
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("aXY", result.get(0));
        Assertions.assertEquals("bXY", result.get(1));
        Assertions.assertEquals("cXY", result.get(2));
        Assertions.assertEquals("d", result.get(3));
    }

    @Test
    public void digitSplitEnd_capture_multiple2() {
        List<String> result = RecordSplitter.splitOnEnd("aX1234YbX5678YcX90Yd", "X([0-9]+)Y");
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("a1234", result.get(0));
        Assertions.assertEquals("b5678", result.get(1));
        Assertions.assertEquals("c90", result.get(2));
        Assertions.assertEquals("d", result.get(3));
    }

    // ========================================================================================================

    @Test
    public void digitSplitStart0() {
        List<String> result = RecordSplitter.splitOnStart("abc", "[0-9]+");
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("abc", result.get(0));
    }

    @Test
    public void digitSplitStart1() {
        List<String> result = RecordSplitter.splitOnStart("a1234b5678c90", "[0-9]+");
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("b", result.get(1));
        Assertions.assertEquals("c", result.get(2));
        Assertions.assertEquals("", result.get(3));
    }

    @Test
    public void digitSplitStart_nocapture1() {
        List<String> result = RecordSplitter.splitOnStart("aX1234bX5678cX90", "X[0-9]+");
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("b", result.get(1));
        Assertions.assertEquals("c", result.get(2));
        Assertions.assertEquals("", result.get(3));
    }

    @Test
    public void digitSplitStart_capturex1() {
        List<String> result = RecordSplitter.splitOnStart("aX1234bX5678cX90", "(X)[0-9]+");
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("Xb", result.get(1));
        Assertions.assertEquals("Xc", result.get(2));
        Assertions.assertEquals("X", result.get(3));
    }

    @Test
    public void digitSplitStart_nocapture2() {
        List<String> result = RecordSplitter.splitOnStart("aX1234bX5678cX90d", "X[0-9]+");
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("b", result.get(1));
        Assertions.assertEquals("c", result.get(2));
        Assertions.assertEquals("d", result.get(3));
    }

    @Test
    public void digitSplitStart_capturex2() {
        List<String> result = RecordSplitter.splitOnStart("aX1234bX5678cX90d", "(X)[0-9]+");
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("Xb", result.get(1));
        Assertions.assertEquals("Xc", result.get(2));
        Assertions.assertEquals("Xd", result.get(3));
    }

    @Test
    public void digitSplitStart_capture_multiple1() {
        List<String> result = RecordSplitter.splitOnStart("aX1234YbX5678YcX90Yd", "(X)[0-9]+(Y)");
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("XYb", result.get(1));
        Assertions.assertEquals("XYc", result.get(2));
        Assertions.assertEquals("XYd", result.get(3));
    }

    @Test
    public void digitSplitStart_capture_multiple2() {
        List<String> result = RecordSplitter.splitOnStart("aX1234YbX5678YcX90Yd", "X([0-9]+)Y");
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("1234b", result.get(1));
        Assertions.assertEquals("5678c", result.get(2));
        Assertions.assertEquals("90d", result.get(3));
    }


}
