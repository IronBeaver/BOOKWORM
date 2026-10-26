import java.util.*;
import javax.swing.text.html.parser.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/** Build-time-only generator using JDK's HTML 4 entity table. Swing is never an Android dependency. */
public final class GenerateEntities {
    public static void main(String[] args) throws Exception {
        new ParserDelegator();
        DTD dtd = DTD.getDTD("html32"); TreeMap<String, String> values = new TreeMap<>();
        for (Object key : dtd.entityHash.keySet()) {
            if (!(key instanceof String)) continue;
            String name = (String) key; Entity entity = dtd.getEntity(name);
            if (name.matches("amp|lt|gt|quot|apos") || entity.getString().length() != 1) continue;
            values.put(name, Integer.toString(entity.getString().charAt(0)));
        }
        StringBuilder data = new StringBuilder(); for (Map.Entry<String,String> e : values.entrySet()) data.append(e.getKey()).append(':').append(e.getValue()).append('|');
        String source = "package com.bookworm.reader;\n"
            + "/** Generated from the JDK HTML entity table by tests/GenerateEntities.java. No external DTD fetches. */\n"
            + "public final class HtmlEntities {\n"
            + " private static final java.util.Map<String,String> ENTITIES = new java.util.HashMap<>();\n"
            + " static { for (String entry : \"" + data + "\".split(\"\\\\|\")) { String[] pair=entry.split(\":\"); if(pair.length==2) ENTITIES.put(pair[0], pair[1]); } }\n"
            + " public static String expand(String input) {\n"
            + "  java.util.regex.Matcher m=java.util.regex.Pattern.compile(\"(?s)<!\\\\[CDATA\\\\[.*?\\\\]\\\\]>|<!--.*?-->|&([A-Za-z][A-Za-z0-9]+);\").matcher(input); StringBuffer out=new StringBuffer();\n"
            + "  while(m.find()){String value=ENTITIES.get(m.group(1)); m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(value==null?m.group():\"&#\"+value+\";\"));} m.appendTail(out); return out.toString();\n }\n}\n";
        Files.write(Paths.get(args[0]), source.getBytes(StandardCharsets.UTF_8)); System.out.println("Generated " + values.size() + " named entities.");
    }
}
