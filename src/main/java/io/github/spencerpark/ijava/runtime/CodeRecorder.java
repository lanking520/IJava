package io.github.spencerpark.ijava.runtime;

import java.util.*;

public class CodeRecorder {

    Map<String, String> classes;
    List<String> imports;
    List<String> mavenDeps;

    public CodeRecorder() {
        classes = new HashMap<>();
        imports = new ArrayList<>();
        mavenDeps = new ArrayList<>();
    }

    public void parse(String content) {
        String[] splitted = content.split("\\s+");
        int iter = 0;
        while (iter < splitted.length) {
            if ("class".equals(splitted[iter])) {
                iter = parseClass(splitted, iter);
            } else if ("import".equals(splitted[iter]) && iter + 1 < splitted.length) {
                imports.add("import " + splitted[iter + 1]);
                iter += 2;
            } else if ("%maven".equals(splitted[iter]) && iter + 1 < splitted.length) {
                mavenDeps.add(splitted[iter+1]);
                iter += 2;
            } else if ("%mavenRepo".equals(splitted[iter]) && iter + 2 < splitted.length) {
                mavenDeps.add(splitted[iter+1] + " " + splitted[iter+2]);
                iter += 3;
            } else {
                iter++;
            }
        }
    }

    private int parseClass(String[] splitted, int iter) {
        StringJoiner joiner = new StringJoiner(" ");
        String name = splitted[++iter];
        joiner.add("class").add(name);
        iter++;
        for (; iter < splitted.length && !splitted[iter].contains("{"); iter++) {
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
        return iter;
    }

    public Map<String, String> getClasses() {
        return classes;
    }

    public List<String> getImports() {
        return imports;
    }

    public List<String> getMavenDeps() {
        return mavenDeps;
    }

}
