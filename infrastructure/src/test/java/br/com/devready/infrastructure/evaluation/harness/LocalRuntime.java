package br.com.devready.infrastructure.evaluation.harness;

import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.ExecutionOutcome;
import br.com.devready.domain.evaluation.ExecutionReport;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class LocalRuntime {

    private static final long TIMEOUT_SECONDS = 30;

    private record Result(int exit, String stdout, String stderr, boolean timedOut) {}

    private LocalRuntime() {}

    static ExecutionReport run(Language language, String program, String stdin, Path dir)
            throws IOException, InterruptedException {
        return switch (language) {
            case JAVA -> java(program, stdin, dir);
            case PYTHON ->
                    interpreted(
                            List.of("python3", "script.py"),
                            dir.resolve("script.py"),
                            program,
                            stdin,
                            dir);
            case JAVASCRIPT ->
                    interpreted(
                            List.of("node", "script.js"),
                            dir.resolve("script.js"),
                            program,
                            stdin,
                            dir);
        };
    }

    private static ExecutionReport java(String program, String stdin, Path dir)
            throws IOException, InterruptedException {
        Files.writeString(dir.resolve("Main.java"), program, StandardCharsets.UTF_8);
        Path bin = Path.of(System.getProperty("java.home"), "bin");
        Result compile =
                exec(
                        List.of(
                                bin.resolve("javac").toString(),
                                "--release",
                                "13",
                                "-encoding",
                                "US-ASCII",
                                "Main.java"),
                        "",
                        dir);
        if (compile.exit() != 0) {
            return new ExecutionReport(
                    ExecutionOutcome.COMPILE_ERROR,
                    "",
                    "",
                    compile.stderr() + compile.stdout(),
                    null);
        }
        return report(
                exec(List.of(bin.resolve("java").toString(), "-cp", ".", "Main"), stdin, dir));
    }

    private static ExecutionReport interpreted(
            List<String> command, Path file, String program, String stdin, Path dir)
            throws IOException, InterruptedException {
        Files.writeString(file, program, StandardCharsets.UTF_8);
        return report(exec(command, stdin, dir));
    }

    private static ExecutionReport report(Result run) {
        ExecutionOutcome outcome =
                run.timedOut()
                        ? ExecutionOutcome.TIME_LIMIT
                        : run.exit() == 0 ? ExecutionOutcome.FINISHED : ExecutionOutcome.CRASHED;
        return new ExecutionReport(
                outcome, run.stdout(), run.stderr(), null, "código de saída " + run.exit());
    }

    private static Result exec(List<String> command, String stdin, Path dir)
            throws IOException, InterruptedException {
        Path in = dir.resolve("stdin.txt");
        Path out = dir.resolve("stdout.txt");
        Path err = dir.resolve("stderr.txt");
        Files.writeString(in, stdin, StandardCharsets.UTF_8);
        Process process =
                new ProcessBuilder(command)
                        .directory(dir.toFile())
                        .redirectInput(in.toFile())
                        .redirectOutput(out.toFile())
                        .redirectError(err.toFile())
                        .start();
        boolean done = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!done) {
            process.destroyForcibly().waitFor();
        }
        return new Result(
                done ? process.exitValue() : -1,
                Files.readString(out, StandardCharsets.UTF_8),
                Files.readString(err, StandardCharsets.UTF_8),
                !done);
    }
}
