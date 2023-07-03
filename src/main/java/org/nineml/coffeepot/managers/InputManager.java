package org.nineml.coffeepot.managers;

import org.nineml.coffeefilter.InvisibleXmlParser;
import org.nineml.coffeefilter.util.URIUtils;
import org.nineml.coffeepot.utils.RecordSplitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InputManager {
    public static final String logcategory = "CoffeePot";
    public static final int UnicodeBOM = 0xFEFF;

    private final Configuration config;
    private final InvisibleXmlParser parser;
    public final List<String> records;

    public InputManager(Configuration config, InvisibleXmlParser parser) throws IOException {
        this.config = config;
        this.parser = parser;

        final String input;
        // Collect the input so we can break it into records
        if (config.inputFile != null) {
            final InputStreamReader isr;
            if ("-".equals(config.inputFile)) {
                config.options.getLogger().debug(logcategory, "Reading standard input");
                isr = new InputStreamReader(System.in, config.encoding);
            } else {
                URI inputURI = URIUtils.resolve(URIUtils.cwd(), config.inputFile);
                config.options.getLogger().debug(logcategory, "Loading input: %s", inputURI);
                URLConnection conn = inputURI.toURL().openConnection();
                isr = new InputStreamReader(conn.getInputStream(), config.encoding);
            }
            BufferedReader reader = new BufferedReader(isr);

            // I'm not confident this is the most efficient way to do this, but...
            StringBuilder sb = new StringBuilder(1024);
            boolean ignoreBOM = config.options.getIgnoreBOM() && "utf-8".equalsIgnoreCase(config.encoding);
            int inputchar = reader.read();
            if (inputchar != -1) {
                if (!ignoreBOM || inputchar != UnicodeBOM) {
                    sb.append((char) inputchar);
                }
                inputchar = reader.read();
            }
            while (inputchar != -1) {
                sb.append((char) inputchar);
                inputchar = reader.read();
            }
            input = sb.toString();
        } else {
            input = config.input;
        }

        boolean hasRecords = false;
        String recordStart = null;
        String recordEnd = null;

        if (config.records) {
            hasRecords = config.records;
            recordStart = config.recordStart;
            recordEnd = config.recordEnd;
        } else {
            // What if the grammar specifies a record separator...
            final String rsuri = "https://nineml.org/ns/pragma/options/record-start";
            final String reuri = "https://nineml.org/ns/pragma/options/record-end";

            Map<String, List<String>> metadata = parser.getMetadata();
            if (metadata.containsKey(rsuri) && metadata.containsKey(reuri)) {
                config.options.getLogger().error(logcategory, "Grammar must not specify both record-start and record-end options.");
            } else if (metadata.containsKey(rsuri) || metadata.containsKey(reuri)) {
                String key = metadata.containsKey(rsuri) ? rsuri : reuri;
                if (metadata.get(key).size() != 1) {
                    config.options.getLogger().error(logcategory, "Grammar must not specify more than one record-start or record-end option.");
                } else {
                    String value = metadata.get(key).get(0).trim();
                    if ("".equals(value)) {
                        config.options.getLogger().error(logcategory, "Grammar must not specify empty record separator.");
                    } else {
                        String quote = value.substring(0, 1);
                        if (quote.equals("\"") || quote.equals("'")) {
                            if (!value.endsWith(quote)) {
                                config.options.getLogger().error(logcategory, "Grammar specified record separator with mismatched quotes.");
                            } else {
                                value = value.substring(1, value.length()-1);
                                if ("'".equals(quote)) {
                                    value = value.replaceAll("\\\\'", quote);
                                } else {
                                    value = value.replaceAll("\\\\\"", quote);
                                }
                                config.options.getLogger().info(logcategory, "Grammar selects record-based processing.");
                                hasRecords = true;
                                if (rsuri.equals(key)) {
                                    recordStart = value;
                                } else {
                                    recordEnd = value;
                                }
                            }
                        } else {
                            config.options.getLogger().info(logcategory, "Grammar selects record-based processing.");
                            hasRecords = true;
                            if (rsuri.equals(key)) {
                                recordStart = value;
                            } else {
                                recordEnd = value;
                            }
                        }
                    }
                }
            }
        }

        if (hasRecords) {
            if (recordEnd != null) {
                records = RecordSplitter.splitOnEnd(input, recordEnd);
            } else {
                records = RecordSplitter.splitOnStart(input, recordStart);
            }
        } else {
            records = new ArrayList<>();
            records.add(input);
        }
    }
}
