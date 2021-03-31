package io.github.spencerpark.ijava.recorder;

import io.github.spencerpark.ijava.runtime.Recorder;

import java.util.ArrayList;
import java.util.List;

public class DependencyRecorder implements Recorder {

    List<String> depsRepo = new ArrayList<>();

    @Override
    public void parse(String content) {
        String[] splitted = content.split("\\s+");
        for (int i = 0; i < splitted.length; i++) {
            if ("%maven".equals(splitted[i]) && i + 1 < splitted.length) {
                depsRepo.add(splitted[i+1]);
            } else if ("%mavenRepo".equals(splitted[i]) && i + 2 < splitted.length) {
                depsRepo.add(splitted[i+1] + " " + splitted[i+2]);
            }
        }
    }

    @Override
    public List<String> getContent() {
        return depsRepo;
    }
}
