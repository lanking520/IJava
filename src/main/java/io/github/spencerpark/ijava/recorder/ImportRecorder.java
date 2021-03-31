package io.github.spencerpark.ijava.recorder;

import io.github.spencerpark.ijava.runtime.Recorder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImportRecorder implements Recorder {

    Set<String> code;

    public ImportRecorder() {
        code = new HashSet<>();
    }

    @Override
    public void parse(String content) {
        String[] splitted = content.split("\\s+");
        // naive filter import xxxx.xxxx.xxx;
        for (int i = 0; i < splitted.length; i++) {
            if ("import".equals(splitted[i]) && i + 1 < splitted.length) {
                code.add("import " + splitted[i+1]);
                i++;
            }
        }
    }

    @Override
    public List<String> getContent() {
        return new ArrayList<>(code);
    }
}
