package org.example;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ReportFeature {
    public static void main(String[] args) {
        // Create frame
        JFrame frame = new JFrame("Get Report");
        frame.setSize(400, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        // Create combobox (dropdown)
        String[] reports = {"Get Report", "Report 1", "Report 2", "Report 3"};
        JComboBox<String> reportDropdown = new JComboBox<>(reports);
        reportDropdown.setBounds(150, 50, 150, 20);
        frame.add(reportDropdown);

        // Add action listener to dropdown
        reportDropdown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (reportDropdown.getSelectedIndex() > 0) {
                    String selectedReport = (String) reportDropdown.getSelectedItem();
                    JOptionPane.showMessageDialog(frame, "Generating " + selectedReport);
                    // Reset the dropdown to the first item (optional)
                    reportDropdown.setSelectedIndex(0);
                    // Add your report generation logic here
                }
            }
        });

        // Set frame visibility
        frame.setVisible(true);
    }

    //    private void updateWorkflowFile() {
//        String owner = ownerField.getText();
//        String repo = repoField.getText();
//        String workflowId = workflowIdField.getText();
//        String token = tokenField.getText();
//        String yamlContent = appendUploadArtifactsJob(outputArea.getText());
//
//        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents/.github/workflows/%s", owner, repo, workflowId);
//
//        try {
//            // Encode the content to Base64
//            String encodedContent = Base64.getEncoder().encodeToString(yamlContent.getBytes(StandardCharsets.UTF_8));
//
//            // Prepare the JSON payload
//            JSONObject jsonPayload = new JSONObject();
//            jsonPayload.put("message", getCommitMessage(consoleArea.getText()));
//            jsonPayload.put("content", encodedContent);
//
//            // Add the SHA of the file to update
//            // Make sure to get this SHA from a previous GET request if the file exists
//            jsonPayload.put("sha", getFileSha(apiUrl, token));
//
//            // Send the PUT request to update the file
//            URL url = new URL(apiUrl);
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("PUT");
//            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
//            conn.setRequestProperty("Authorization", "token " + token);
//            conn.setRequestProperty("Content-Type", "application/json");
//            conn.setDoOutput(true);
//
//            try (OutputStream os = conn.getOutputStream()) {
//                byte[] input = jsonPayload.toString().getBytes(StandardCharsets.UTF_8);
//                os.write(input, 0, input.length);
//            }
//
//            // Check response
//            int responseCode = conn.getResponseCode();
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                consoleArea.setText("Workflow file updated successfully.");
//            } else {
//                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
//                StringBuilder errorResponse = new StringBuilder();
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    errorResponse.append(line).append("\n");
//                }
//                reader.close();
//                consoleArea.setText("Failed to update workflow file: " + responseCode + " - " + errorResponse.toString());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            consoleArea.setText("Error: " + e.getMessage());
//        }
//    }
}
