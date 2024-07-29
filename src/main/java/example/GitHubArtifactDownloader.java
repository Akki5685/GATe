package example;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import org.json.JSONArray;

public class GitHubArtifactDownloader {

    private static final String REPO_OWNER = "Akki5685";
    private static final String REPO_NAME = "gitActions";
    private static final String GITHUB_TOKEN = "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";

    public static void main(String[] args) throws Exception {
        long runId = 10143174740L;
        downloadArtifact(getArtifactId(runId));
    }

    private static int getArtifactId(long runId) throws Exception {
        URL url = new URL("https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/actions/runs/" + runId + "/artifacts");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "token " + GITHUB_TOKEN);

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

    private static void downloadArtifact(long artifactId) throws Exception {
        URL url = new URL("https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/actions/artifacts/" + artifactId + "/zip");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "token " + GITHUB_TOKEN);

        InputStream is = conn.getInputStream();
        FileOutputStream fos = new FileOutputStream("artifact.zip");

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }

        fos.close();
        is.close();
    }
}
