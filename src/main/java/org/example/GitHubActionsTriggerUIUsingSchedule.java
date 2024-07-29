package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GitHubActionsTriggerUIUsingSchedule extends JFrame {
    private static final String PROPERTIES_FILE = "app.properties";
    private JTextField ownerField;
    private JTextField repoField;
    private JTextField workflowIdField;
    private JComboBox<String> branchComboBox;
    private JComboBox<String> reportDropdown;
    private static JTextField tokenField;
    private static JTextArea outputArea;
    private static JTextArea consoleArea;
    private ScheduledExecutorService scheduler;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GitHubActionsTriggerUIUsingSchedule::new);
    }

    public GitHubActionsTriggerUIUsingSchedule() {
        setTitle("GitHub Actions Trigger");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 700);

        Container container = getContentPane();
        container.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Labels and fields arrangement
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        container.add(new JLabel("Owner:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        ownerField = new JTextField(30);
        container.add(ownerField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        container.add(new JLabel("Repository:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        repoField = new JTextField(30);
        container.add(repoField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        container.add(new JLabel("Personal Access Token:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        tokenField = new JTextField(30);
        container.add(tokenField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        container.add(new JLabel("Branch:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        branchComboBox = new JComboBox<>();
        branchComboBox.setPreferredSize(new Dimension(200, branchComboBox.getPreferredSize().height));
        container.add(branchComboBox, gbc);

        JButton loadBranchesButton = new JButton("Load Branches");
        loadBranchesButton.setPreferredSize(new Dimension(150, loadBranchesButton.getPreferredSize().height));
        loadBranchesButton.addActionListener(e -> loadBranches());
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        container.add(loadBranchesButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        container.add(new JLabel("Workflow ID:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        workflowIdField = new JTextField(30);
        container.add(workflowIdField, gbc);

        // Panel for buttons
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        JButton updateButton = new JButton("Update Workflow");
        updateButton.addActionListener(e -> {
            try {
                createOrUpdateFile();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        buttonPanel.add(updateButton);

        JButton triggerButton = new JButton("Trigger Workflow");
        triggerButton.addActionListener(e -> {
            // Cancel the previous scheduler if any
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
            // Initialize scheduler
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.schedule(this::triggerWorkflow, 0, TimeUnit.SECONDS);
        });
        buttonPanel.add(triggerButton);

        JButton fetchButton = new JButton("Fetch Workflow");
        fetchButton.addActionListener(e -> fetchWorkflowDetails());
        buttonPanel.add(fetchButton);

        // Create combobox (dropdown) for reports

        String[] reports = {"Get Report"};
        reportDropdown = new JComboBox<>(reports);
        reportDropdown.setPreferredSize(new Dimension(150, reportDropdown.getPreferredSize().height));
        reportDropdown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (reportDropdown.getSelectedIndex() > 0) {
                    String selectedReport = (String) reportDropdown.getSelectedItem();
                    JOptionPane.showMessageDialog(GitHubActionsTriggerUIUsingSchedule.this, "Generating Report");
                    try {
                        downloadArtifact(getArtifactId(Long.parseLong(selectedReport)));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    // Reset the dropdown to the first item (optional)
                    reportDropdown.setSelectedIndex(0);
                    // Add your report generation logic here
                }
            }
        });

        // Add report dropdown to button panel
        buttonPanel.add(reportDropdown);

        container.add(buttonPanel, gbc);

        // Output Area
        JLabel outputLabel = new JLabel("Work Flow:");
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        container.add(outputLabel, gbc);

        outputArea = new JTextArea(10, 50);
        outputArea.setEditable(true);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        gbc.gridy = 7;
        container.add(outputScrollPane, gbc);

        // Console Area
        JLabel consoleLabel = new JLabel("Commit and Status:");
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        container.add(consoleLabel, gbc);

        consoleArea = new JTextArea(5, 50);
        consoleArea.setEditable(true);
        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
        gbc.gridy = 9;
        container.add(consoleScrollPane, gbc);

        setVisible(true);

        // Load saved data
        loadProperties();

        // Add shutdown hook to save data when closing the application
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveProperties));

        loadBranches();
    }

    private void loadProperties() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            props.load(in);
            ownerField.setText(props.getProperty("owner", ""));
            repoField.setText(props.getProperty("repo", ""));
            workflowIdField.setText(props.getProperty("workflowId", ""));
            tokenField.setText(props.getProperty("token", ""));
            outputArea.setText(props.getProperty("output", ""));
            consoleArea.setText(props.getProperty("console", ""));
        } catch (IOException e) {
            // Continue without data if file not found or read error occurs
            System.out.println("No properties file found or an error occurred. Continuing without loading data.");
        }
    }

    private void saveProperties() {
        Properties props = new Properties();
        props.setProperty("owner", ownerField.getText());
        props.setProperty("repo", repoField.getText());
        props.setProperty("workflowId", workflowIdField.getText());
        props.setProperty("token", tokenField.getText());
        props.setProperty("output", outputArea.getText());
        props.setProperty("console", consoleArea.getText());
        try (FileOutputStream out = new FileOutputStream(PROPERTIES_FILE)) {
            props.store(out, "GitHub Actions Trigger UI Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadBranches() {
        String owner = ownerField.getText().trim();
        String repo = repoField.getText().trim();
        String token = tokenField.getText().trim();

        String apiUrl = String.format("https://api.github.com/repos/%s/%s/branches", owner, repo);

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Authorization", "token " + token);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                InputStream inputStream = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray branches = new JSONArray(response.toString());

                // Ensure that the GUI updates are done on the Event Dispatch Thread
                SwingUtilities.invokeLater(() -> {
                    branchComboBox.removeAllItems();
                    for (int i = 0; i < branches.length(); i++) {
                        JSONObject branch = branches.getJSONObject(i);
                        branchComboBox.addItem(branch.getString("name"));
                    }
                    consoleArea.setText("Branches loaded successfully.");
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    consoleArea.setText("Failed to load branches: " + responseCode);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                consoleArea.setText("An error occurred while loading branches: " + e.getMessage());
            });
        }
    }

    private String appendUploadArtifactsJob(String content) {
        // Define the new job to upload artifacts
        String uploadArtifactsJob = "  upload-artifacts:\n"
                + "    runs-on: self-hosted\n"
                + "    needs: print\n"
                + "    if: always()\n" // Ensure this job runs regardless of previous job status
                + "    steps:\n"
                + "      - name: Upload test reports\n"
                + "        uses: actions/upload-artifact@v3\n"
                + "        with:\n"
                + "          name: test-reports\n"
                + "          path: ${{ github.workspace }}/TestReports\n";

        // Check if the job already exists in the content
        if (content.contains("upload-artifacts:")) {
            outputArea.setText("Job 'upload-artifacts' already exists in the content.");
            return content;
        }

        // Find the index of the last job section
        int jobsSectionIndex = content.lastIndexOf("jobs:");

        // If 'jobs:' is found, append the new job after it
        if (jobsSectionIndex != -1) {
            // Insert the new job after the last 'jobs:' occurrence
            String beforeJobs = content.substring(0, jobsSectionIndex + 5); // Include the length of 'jobs:'
            String afterJobs = content.substring(jobsSectionIndex + 5);
            return beforeJobs + "\n" + uploadArtifactsJob + "\n" + afterJobs;
        } else {
            // Handle case where 'jobs:' is not found
            System.out.println("Keyword 'jobs:' not found in the content.");
            return content;
        }
    }


    public String getCommitMessage(String message) {
        if (message.contains("\"")) {
            return message.replace("\"", "");
        } else {
            return "Updated the workflow";
        }
    }

    private void triggerWorkflow() {
        String owner = ownerField.getText().trim();
        String repo = repoField.getText().trim();
        String workflowId = workflowIdField.getText().trim();
        String token = tokenField.getText().trim();
        String branch = (String) branchComboBox.getSelectedItem();

        String apiUrl = String.format("https://api.github.com/repos/%s/%s/actions/workflows/%s/dispatches", owner, repo, workflowId);

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Authorization", "token " + token);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            JSONObject payload = new JSONObject();
            payload.put("ref", branch);

            byte[] postDataBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
            conn.getOutputStream().write(postDataBytes);

            int responseCode = conn.getResponseCode();
            if (responseCode == 204) {
                consoleArea.setText("Workflow triggered successfully.");
            } else {
                consoleArea.setText("Failed to trigger workflow: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
            consoleArea.setText("An error occurred while triggering workflow: " + e.getMessage());
        }
    }

    private void fetchWorkflowRunStatus() {
        String owner = ownerField.getText().trim();
        String repo = repoField.getText().trim();
        String token = tokenField.getText().trim();
        String workflowId = workflowIdField.getText().trim();

        String apiUrl = String.format("https://api.github.com/repos/%s/%s/actions/runs?workflow_id=%s", owner, repo, workflowId);

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Authorization", "token " + token);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                InputStream inputStream = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray workflowRuns = jsonResponse.getJSONArray("workflow_runs");

                StringBuilder statusBuilder = new StringBuilder();

                reportDropdown.removeAllItems();
                reportDropdown.addItem("Get Report");
                for (int i = 0; i < workflowRuns.length(); i++) {
                    JSONObject run = workflowRuns.getJSONObject(i);
                    long runId = run.getLong("id");
                    String status = run.getString("status");
                    String conclusion = run.optString("conclusion", "in progress");

                    reportDropdown.addItem(String.valueOf(runId));

                    statusBuilder.append(String.format("WorkFlow-ID: %d is %s - %s%n", runId, status, conclusion));
                }

                SwingUtilities.invokeLater(() -> consoleArea.setText(statusBuilder.toString()));
            } else {
                SwingUtilities.invokeLater(() -> consoleArea.setText("Failed to fetch workflow run status: " + responseCode));
            }
        } catch (IOException e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> consoleArea.setText("An error occurred while fetching workflow run status: " + e.getMessage()));
        }
    }

    private void fetchWorkflowDetails() {
        String owner = ownerField.getText();
        String repo = repoField.getText();
        String workflowId = workflowIdField.getText();
        String token = tokenField.getText();

        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents/.github/workflows/%s", owner, repo, workflowId);

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Authorization", "token " + token);

            // Read response
            InputStream inputStream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            reader.close();

            // Decode Base64 content
            JSONObject jsonResponse = new JSONObject(response.toString());
            String contentBase64 = jsonResponse.getString("content");

            // Remove any unwanted characters or padding
            contentBase64 = contentBase64.replaceAll("[^A-Za-z0-9+/=]", "");

            byte[] decodedBytes;
            try {
                decodedBytes = Base64.getDecoder().decode(contentBase64);
            } catch (IllegalArgumentException e) {
                outputArea.setText("Failed to decode Base64 content. Content may be improperly encoded.");
                e.printStackTrace();
                return;
            }

            String yamlContent = new String(decodedBytes, StandardCharsets.UTF_8);

            // Display YAML content in the GUI
            outputArea.setText(yamlContent);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                consoleArea.setText("Fetched the workflow: " + responseCode);
                // Already set text in outputArea above
            } else {
                consoleArea.setText("Failed to fetch workflow details: " + responseCode);
            }
            fetchWorkflowRunStatus();
        } catch (Exception e) {
            e.printStackTrace();
            consoleArea.setText("Error: " + e.getMessage());
        }
    }

    private  void createOrUpdateFile() throws IOException {
        // Step 1: Get the current file SHA (if it exists)
        String sha = getFileSha(workflowIdField.getText());

        // Step 2: Create or update the file
        URL url = new URL("https://api.github.com/repos/" + ownerField.getText() + "/" + repoField.getText() + "/contents/.github/workflows/" + workflowIdField.getText());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Authorization", "token " + tokenField.getText());
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("message", getCommitMessage(consoleArea.getText()));
        jsonBody.put("content", new String(java.util.Base64.getEncoder().encode(outputArea.getText().getBytes(StandardCharsets.UTF_8))));
        if (sha != null) {
            jsonBody.put("sha", sha);
        }

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_CREATED && responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to create/update file. HTTP response code: " + responseCode);
        }
    }
    private String getFileSha(String apiUrl, String token) throws IOException {
        // Retrieve the SHA of the file by making a GET request
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setRequestProperty("Authorization", "token " + token);

        InputStream inputStream = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
        }
        reader.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getString("sha");
    }

    private int getArtifactId(long runId) throws Exception {
        URL url = new URL("https://api.github.com/repos/" + ownerField.getText() + "/" + repoField.getText() + "/actions/runs/" + runId + "/artifacts");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "token " + tokenField.getText());

        InputStream is = conn.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        byte[] bytes = bis.readAllBytes();
        String response = new String(bytes);

        JSONObject jsonResponse = new JSONObject(response);
        JSONArray artifacts = jsonResponse.getJSONArray("artifacts");
        if (artifacts.length() > 0) {
            return artifacts.getJSONObject(0).getInt("id");
        } else {
            throw new RuntimeException("No artifacts found");
        }
    }

    private void downloadArtifact(long artifactId) throws Exception {
        URL url = new URL("https://api.github.com/repos/" + ownerField.getText() + "/" + repoField.getText() + "/actions/artifacts/" + artifactId + "/zip");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "token " + tokenField.getText());

        InputStream is = conn.getInputStream();
        String userHome = System.getProperty("user.home");
        Path desktopPath = Paths.get(userHome, "Desktop");
        // Construct the path to the report.zip file
        Path filePath = Paths.get(desktopPath.toString(), "Report.zip");

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            // Write data to the file
            // fos.write(data); // Replace with actual data to write
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            fos.close();
            is.close();

            System.out.println("File created at: " + filePath.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private  String getFileSha(String path) throws IOException {
        URL url = new URL("https://api.github.com/repos/" + ownerField.getText() + "/" + repoField.getText() + "/contents/" + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "token " + tokenField.getText());
        connection.setRequestProperty("Accept", "application/vnd.github+json");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                JSONObject jsonResponse = new JSONObject(response.toString());
                return jsonResponse.optString("sha", null);
            }
        } else {
            return null;
        }
    }
}




