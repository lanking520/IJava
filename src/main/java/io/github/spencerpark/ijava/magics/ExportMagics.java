package io.github.spencerpark.ijava.magics;

import io.github.spencerpark.ijava.execution.CodeEvaluator;
import io.github.spencerpark.ijava.magics.utils.ZipUtils;
import io.github.spencerpark.ijava.recorder.ClassRecorder;
import io.github.spencerpark.ijava.recorder.DependencyRecorder;
import io.github.spencerpark.ijava.recorder.ImportRecorder;
import io.github.spencerpark.ijava.runtime.Recorder;
import io.github.spencerpark.jupyter.kernel.magic.registry.LineMagic;
import org.codehaus.plexus.util.FileUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;

public class ExportMagics {

    ImportRecorder ir;
    ClassRecorder cr;
    DependencyRecorder dr;
    CodeEvaluator eva;

    public ExportMagics(List<Recorder> recorders, CodeEvaluator evaluator) {
        for (Recorder recorder : recorders) {
            if (recorder instanceof ImportRecorder) {
                ir = (ImportRecorder) recorder;
            } else if (recorder instanceof ClassRecorder) {
                cr = (ClassRecorder) recorder;
            } else if (recorder instanceof DependencyRecorder) {
                dr = (DependencyRecorder) recorder;
            }
        }
        if (ir == null || cr == null || dr == null) {
            throw new RuntimeException("Recorder not registered!");
        }
        this.eva = evaluator;
    }

    @LineMagic(aliases = "export")
    public void exec(List<String> args) throws IOException {
        if (args.size() != 1) {
            throw new RuntimeException("Require single name input");
        }
        Path src = Paths.get("template/gradle_project");
        Path dest;
        try {
            dest = Files.createTempDirectory("template");
            FileUtils.copyDirectoryStructure(src.toFile(), dest.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Create temp dir failed");
        }
        System.out.println("Start gathering parameters...");
        exportArtifact(args.get(0), dest);
        System.out.println("Template created in: " + dest.toString());
        writeCode(dest);
        writeDeps(dest);
        System.out.println("Start generation...");
        buildJar(dest);
        System.out.println("Build completed! Zipping everything...");
        Path zipFile = zipContent(dest);
        System.out.println("Finished! you can find the zip file here: " + zipFile.toAbsolutePath().toString());
    }

    private void exportArtifact(String modelName, Path dest) {
        try {
            eva.eval("import java.nio.file.*;");
            ir.parse("import java.nio.file.*;");
            String code = modelName + ".save(Paths.get(\"" + dest.toAbsolutePath().toString() + "\"), \"exported\")";
            System.out.println(code);
            eva.eval(code);
        } catch (Exception e) {
            throw new RuntimeException("Export failed, due to", e);
        }
    }

    private void writeCode(Path dest) {
        try {
            Path dir = dest.resolve("src/main/java/ai/djl/examples");
            Files.createDirectories(dir);
            BufferedWriter writer = Files.newBufferedWriter(dir.resolve("Export.java"));
            writer.write("package ai.djl.examples;\n");
            writer.write(String.join("\n", ir.getContent()));
            writer.write("\n" + String.join("\n", cr.getContent()));
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("cannot write to file export.java", e);
        }
    }

    private void writeDeps(Path dest) {
        try(BufferedWriter writer = Files.newBufferedWriter(dest.resolve("build.gradle"), StandardOpenOption.APPEND)) {
            StringJoiner deps = new StringJoiner("\n");
            StringJoiner repo = new StringJoiner("\n");
            deps.add("\ndependencies {");
            repo.add("\nrepositories {");
            for (String line : dr.getContent()) {
                String[] splitted = line.split("\\s+");
                if(splitted.length == 1) {
                    String depName = splitted[0];
                    if (depName.startsWith("ai.djl")) {
                        deps.add("exclusion \"" + depName + "\"");
                    }
                    deps.add("implementation \"" + depName + "\"");
                } else {
                    repo.add("maven {\n")
                            .add("url \"" + splitted[1] + "\"\n}");
                }
            }
            deps.add("}\n");
            repo.add("jcenter()\n}");
            writer.write(repo.toString());
            writer.write(deps.toString());
        } catch (IOException e) {
            throw new RuntimeException("failed to append to build.gradle", e);
        }
    }

    private void buildJar(Path dest) throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        // TODO: add windows capability
        pb.directory(dest.toFile());
        pb.command(dest.resolve("gradlew").toAbsolutePath().toString(), "jar");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (Scanner scanner = new Scanner(process.getInputStream(), StandardCharsets.UTF_8)) {
            while (scanner.hasNext()) {
                System.out.println(scanner.nextLine());
            }
        }
    }

    private Path zipContent(Path dest) throws IOException {
        String targetFile = "exported-0000.params";
        Path dir = Files.createDirectory(dest.resolve("exported"));
        Files.copy(dest.resolve(targetFile), dir.resolve(targetFile));
        // TODO: add jar to the zip
        Path zipFile = dest.resolve("exported.zip");
        ZipUtils.zip(dir, zipFile, true);
        return zipFile;
    }
}
