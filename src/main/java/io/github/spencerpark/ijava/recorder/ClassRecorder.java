package io.github.spencerpark.ijava.recorder;

import io.github.spencerpark.ijava.runtime.Recorder;

import java.util.*;

public class ClassRecorder implements Recorder {

    Map<String, String> classes;

    public ClassRecorder() {
        classes = new HashMap<>();
    }

    @Override
    public void parse(String content) {
        String[] splitted = content.split("\\s+");
        int iter = 0;
        while (iter < splitted.length) {
            // naive class parser
            if ("class".equals(splitted[iter])) {
                StringJoiner joiner = new StringJoiner(" ");
                String name = splitted[++iter];
                joiner.add("class").add(name);
                while (!splitted[++iter].contains("{")) {
                    joiner.add(splitted[iter]);
                }
                joiner.add(splitted[iter]);
                // assume } not in the same String with {
                // assume } does not following with other code
                int counter = 1;
                iter++;
                while (counter != 0 && iter < splitted.length) {
                    for (char value : splitted[iter].toCharArray()) {
                        if (value == '{') {
                            counter++;
                        } else if (value == '}') {
                            counter--;
                        }
                    }
                    joiner.add(splitted[iter]);
                    iter++;
                }
                if (counter == 0) {
                    classes.put(name, joiner.toString());
                }
            }
            iter++;
        }
    }

    @Override
    public List<String> getContent() {
        return new ArrayList<>(classes.values());
    }

    public Map<String, String> getClasses() {
        return classes;
    }

}
