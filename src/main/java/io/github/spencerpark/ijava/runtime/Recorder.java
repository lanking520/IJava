package io.github.spencerpark.ijava.runtime;

import java.util.List;

public interface Recorder {

    void parse(String content);

    List<String> getContent();
}
