package org.nineml.coffeepot.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecordSplitter {
    public static List<String> splitOnStart(String input, String startre) {
        ArrayList<String> strings = new ArrayList<>();

        Pattern re = Pattern.compile(startre);
        Matcher matcher = re.matcher(input);

        // Slightly complicated because we have to catch the last group
        // First, grab everthing before the first capture group
        boolean more = matcher.find();
        if (more) {
            strings.add(input.substring(0, matcher.start()));
        } else {
            strings.add(input);
        }

        while (more) {
            StringBuilder sb = new StringBuilder();
            for (int count = 1; count <= matcher.groupCount(); count++) {
                sb.append(matcher.group(count));
            }
            int startpos = matcher.end();
            more = matcher.find();
            if (more) {
                sb.append(input, startpos, matcher.start());
            } else {
                sb.append(input, startpos, input.length());
            }
            strings.add(sb.toString());
        }

        return strings;
    }

    public static List<String> splitOnEnd(String input, String endre) {
        ArrayList<String> strings = new ArrayList<>();

        Pattern re = Pattern.compile(endre);
        Matcher matcher = re.matcher(input);
        int startpos = 0;
        while (matcher.find()) {
            StringBuilder sb = new StringBuilder();
            sb.append(input, startpos, matcher.start());
            for (int count = 1; count <= matcher.groupCount(); count++) {
                sb.append(matcher.group(count));
            }

            startpos = matcher.end();
            strings.add(sb.toString());
        }
        if (startpos < input.length()) {
            strings.add(input.substring(startpos));
        }

        return strings;
    }
}
