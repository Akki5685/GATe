package example;

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

                branchComboBox.removeAllItems();
                for (int i = 0; i < branches.length(); i++) {
                    JSONObject branch = branches.getJSONObject(i);
                    branchComboBox.addItem(branch.getString("name"));
                }

                appendToConsole("Branches loaded successfully.");
            } else {
                appendToConsole("Failed to load branches. Response code: " + responseCode);
            }

            conn.disconnect();
        } catch (Exception e) {
            appendToConsole("Error loading branches: " + e.getMessage());
        }
    }

    private void createOrUpdateFile() throws IOException {
        Path filePath = Paths.get("workflow.yml");
        if (!java.nio.file.Files.exists(filePath)) {
            java.nio.file.Files.createFile(filePath);
        }
        // Construct your workflow YAML content
        String content = outputArea.getText();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            writer.write(content);
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
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);

            String jsonInputString = "{\"ref\": \"" + branch + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 204) {
                appendToConsole("Workflow triggered successfully.");
            } else {
                appendToConsole("Failed to trigger workflow. Response code: " + responseCode);
            }

            conn.disconnect();
        } catch (Exception e) {
            appendToConsole("Error triggering workflow: " + e.getMessage());
        }
    }

    private void fetchWorkflowDetails() {
        String owner = ownerField.getText().trim();
        String repo = repoField.getText().trim();
        String workflowId = workflowIdField.getText().trim();
        String token = tokenField.getText().trim();
        String branch = (String) branchComboBox.getSelectedItem();

        String apiUrl = String.format("https://api.github.com/repos/%s/%s/actions/workflows/%s", owner, repo, workflowId);

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Authorization", "Bearer " + token);

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

                JSONObject workflow = new JSONObject(response.toString());
                outputArea.setText(workflow.toString(2));
                appendToConsole("Workflow details fetched successfully.");
            } else {
                appendToConsole("Failed to fetch workflow details. Response code: " + responseCode);
            }

            conn.disconnect();
        } catch (Exception e) {
            appendToConsole("Error fetching workflow details: " + e.getMessage());
        }
    }

    private void appendToConsole(String message) {
        consoleArea.append(message + "\n");
    }

    private void downloadArtifact(long artifactId) {
        String owner = ownerField.getText().trim();
        String repo = repoField.getText().trim();
        String token = tokenField.getText().trim();
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/actions/artifacts/%d/zip", owner, repo, artifactId);

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                InputStream inputStream = conn.getInputStream();
                String filePath = Paths.get("artifact_" + artifactId + ".zip").toString();
                try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    appendToConsole("Artifact downloaded successfully to " + filePath);
                }
            } else {
                appendToConsole("Failed to download artifact. Response code: " + responseCode);
            }

            conn.disconnect();
        } catch (Exception e) {
            appendToConsole("Error downloading artifact: " + e.getMessage());
        }
    }

    private long getArtifactId(long workflowId) {
        String owner = ownerField.getText().trim();
        String repo = repoField.getText().trim();
        String token = tokenField.getText().trim();
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/actions/workflows/%d/artifacts", owner, repo, workflowId);

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("Authorization", "Bearer " + token);

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

                JSONArray artifacts = new JSONArray(response.toString());

                if (artifacts.length() > 0) {
                    JSONObject artifact = artifacts.getJSONObject(0);
                    return artifact.getLong("id");
                } else {
                    appendToConsole("No artifacts found.");
                }
            } else {
                appendToConsole("Failed to get artifact ID. Response code: " + responseCode);
            }

            conn.disconnect();
        } catch (Exception e) {
            appendToConsole("Error getting artifact ID: " + e.getMessage());
        }

        return 0;
    }
}
