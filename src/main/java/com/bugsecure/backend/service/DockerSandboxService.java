package com.bugsecure.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class DockerSandboxService {

    @Autowired
    private DockerCliExecutor dockerCli;

    @Value("${sandbox.docker.image:python:3.12-alpine}")
    private String sandboxImage;

    @Value("${sandbox.docker.container-port:8080}")
    private int containerPort;

    @Value("${sandbox.docker.cpus:0.5}")
    private String cpus;

    @Value("${sandbox.docker.memory:256m}")
    private String memory;

    @Value("${sandbox.docker.pids-limit:64}")
    private String pidsLimit;

    @Value("${sandbox.docker.cap-drop:ALL}")
    private String capDrop;

    @Value("${sandbox.docker.no-new-privileges:true}")
    private boolean noNewPrivileges;

    @Value("${sandbox.docker.network-mode:bridge}")
    private String networkMode;

    @Value("${sandbox.docker.target-dir:/sandbox/www}")
    private String containerTargetDir;

    @Value("${sandbox.docker.start-timeout-ms:30000}")
    private long dockerStartTimeoutMs;

    @Value("${sandbox.docker.logs-tail:20000}")
    private int logsTailChars;

    public String getSandboxImage() {
        return sandboxImage;
    }

    public void createCopyAndStart(String containerName, int hostPort, Path packagedSubmissionDir) {
        if (containerName == null || containerName.isBlank()) {
            throw new IllegalArgumentException("containerName is required");
        }
        if (packagedSubmissionDir == null) {
            throw new IllegalArgumentException("packagedSubmissionDir is required");
        }

        // docker create (do not start yet) - run a sleep command so we can docker cp files safely.
        // We create /sandbox/www and then sleep.
        List<String> createCmd = new ArrayList<>();
        createCmd.add("docker");
        createCmd.add("create");
        createCmd.add("--name");
        createCmd.add(containerName);

        createCmd.add("--cpus");
        createCmd.add(cpus);

        createCmd.add("--memory");
        createCmd.add(memory);

        createCmd.add("--pids-limit");
        createCmd.add(pidsLimit);

        if (capDrop != null && !capDrop.isBlank()) {
            createCmd.add("--cap-drop");
            createCmd.add(capDrop);
        }

        if (noNewPrivileges) {
            createCmd.add("--security-opt");
            createCmd.add("no-new-privileges");
        }

        if (networkMode != null && !networkMode.isBlank()) {
            createCmd.add("--network");
            createCmd.add(networkMode);
        }

        // Publish containerPort to a host port so the frontend can open the sandbox URL.
        createCmd.add("-p");
        createCmd.add(hostPort + ":" + containerPort);

        createCmd.add(sandboxImage);
        createCmd.add("sh");
        createCmd.add("-c");
        // Start a static server once the container starts, while also blocking outbound traffic.
        // This is best-effort isolation for v1 (no published internet access from inside the sandbox).
        createCmd.add(
                "set -e; " +
                "(command -v iptables >/dev/null 2>&1 && " +
                "iptables -P OUTPUT ACCEPT && " +
                "iptables -F OUTPUT && " +
                "iptables -A OUTPUT -o lo -j ACCEPT && " +
                "iptables -A OUTPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT && " +
                "iptables -A OUTPUT -j DROP) || true; " +
                "mkdir -p " + containerTargetDir + " /sandbox/work; " +
                "python3 -m http.server " + containerPort + " --bind 0.0.0.0 --directory " + containerTargetDir + " >/dev/null 2>&1 & " +
                "tail -f /dev/null"
        );

        dockerCli.execute(createCmd, Duration.ofMillis(dockerStartTimeoutMs));

        try {
            // Copy packaged files into container. The packaged directory has sessionDir/www/*.
            Path wwwDir = packagedSubmissionDir.resolve("www");
            String hostPathForDocker = wwwDir.toAbsolutePath().toString().replace("\\", "/");
            List<String> cpCmd = new ArrayList<>();
            cpCmd.add("docker");
            cpCmd.add("cp");
            // Copy directory contents into container target dir.
            // `/.` copies the contents of the directory into the destination directory.
            cpCmd.add(hostPathForDocker + "/.");
            cpCmd.add(containerName + ":" + containerTargetDir + "/");

            try {
                dockerCli.execute(cpCmd, Duration.ofMillis(dockerStartTimeoutMs));
            } catch (RuntimeException firstAttempt) {
                List<String> cpCmd2 = new ArrayList<>();
                cpCmd2.add("docker");
                cpCmd2.add("cp");
                cpCmd2.add(hostPathForDocker);
                cpCmd2.add(containerName + ":" + containerTargetDir + "/");
                dockerCli.execute(cpCmd2, Duration.ofMillis(dockerStartTimeoutMs));
            }

            // Start the container to serve /sandbox/www on containerPort.
            List<String> startCmd = List.of("docker", "start", containerName);
            dockerCli.execute(startCmd, Duration.ofMillis(dockerStartTimeoutMs));
        } catch (RuntimeException e) {
            // Best-effort cleanup if anything failed during copy/start.
            stopQuietly(containerName);
            throw e;
        }
    }

    public String stopAndRemove(String containerName) {
        if (containerName == null || containerName.isBlank()) return "";

        String logs = "";
        try {
            List<String> logsCmd = List.of("docker", "logs", "--tail", String.valueOf(logsTailChars), containerName);
            DockerCliExecutor.Result r = dockerCli.execute(logsCmd, Duration.ofMillis(15000));
            logs = r.getStdout() != null ? r.getStdout() : "";
        } catch (Exception ignored) {
        }

        try {
            // Stop first (short timeout), then remove.
            List<String> stopCmd = List.of("docker", "stop", "-t", "5", containerName);
            dockerCli.execute(stopCmd, Duration.ofMillis(20000));
        } catch (Exception ignored) {
        }

        try {
            List<String> rmCmd = List.of("docker", "rm", "-f", containerName);
            dockerCli.execute(rmCmd, Duration.ofMillis(20000));
        } catch (Exception ignored) {
        }

        return logs;
    }

    public void stopQuietly(String containerName) {
        try {
            stopAndRemove(containerName);
        } catch (Exception ignored) {
        }
    }
}

