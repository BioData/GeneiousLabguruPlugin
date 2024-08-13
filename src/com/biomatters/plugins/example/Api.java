package com.biomatters.plugins.example;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Api {
    private String upload_file = "https://prod-flow.labguru.com/flows/2390/flow_runs/external_trigger.json?token=193137b5c3734e70d4f8b";

    private static String boundary =  "===" + System.currentTimeMillis() + "===";
    static HttpURLConnection httpConn;
    static DataOutputStream request;
    static final String crlf = "\r\n";
    static final String twoHyphens = "--";

    public String postattachments(File file, String title, Boolean to_remove) throws IOException {

        URL url = new URL(this.upload_file);
        /**
         * This constructor initializes a new HTTP POST request with content type
         * is set to multipart/form-data
         *
         * @param requestURL
         * @throws IOException
         */

        String filename = file.getAbsolutePath();
        File uploadFile = file;

        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true); // indicates POST method
        httpConn.setDoInput(true);

        httpConn.setRequestMethod("POST");
        httpConn.setRequestProperty("Connection", "Keep-Alive");
        httpConn.setRequestProperty("Cache-Control", "no-cache");
        httpConn.setRequestProperty(
                "Content-Type", "multipart/form-data;boundary=" + boundary);

        request =  new DataOutputStream(httpConn.getOutputStream());
        request.writeBytes(twoHyphens + boundary + crlf);
        request.writeBytes("Content-Disposition: form-data; name=\"" + "attachment_from_trigger" + "\""+ crlf);

        request.writeBytes("Content-Type: text/plain; charset=UTF-8" + crlf);
        request.writeBytes(crlf);
        request.writeBytes(uploadFile.getName()+ crlf);
        request.flush();


        request.writeBytes(twoHyphens + boundary + crlf);
        request.writeBytes("Content-Disposition: form-data; name=\"" + "title" + "\""+ crlf);

        request.writeBytes("Content-Type: text/plain; charset=UTF-8" + crlf);
        request.writeBytes(crlf);
        request.writeBytes(title + crlf);
        request.flush();

        request.writeBytes(twoHyphens + boundary + crlf);
        request.writeBytes("Content-Disposition: form-data; name=\"" + "to_remove" + "\""+ crlf);

        request.writeBytes("Content-Type: text/plain; charset=UTF-8" + crlf);
        request.writeBytes(crlf);
        request.writeBytes(to_remove + crlf);
        request.flush();

        String fileName = uploadFile.getName();
        request.writeBytes(twoHyphens + boundary + crlf);
        request.writeBytes("Content-Disposition: form-data; name=\"" +
                "attachment_from_trigger" + "\";filename=\"" +
                fileName + "\"" + crlf);
        request.writeBytes(crlf);

        byte[] bytes = Files.readAllBytes(Paths.get(uploadFile.getAbsolutePath()));

        request.write(bytes);

        String response ="";
        request.writeBytes(crlf);
        request.writeBytes(twoHyphens + boundary +
                twoHyphens + crlf);

        request.flush();
        request.close();

        // checks server's status code first
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED) {
            InputStream responseStream = new
                    BufferedInputStream(httpConn.getInputStream());

            BufferedReader responseStreamReader =
                    new BufferedReader(new InputStreamReader(responseStream));

            String line = "";
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            responseStreamReader.close();

            response = stringBuilder.toString();
            httpConn.disconnect();
        } else {
            throw new IOException("Server returned non-OK status: " + status);
        }

        return response;
    }

}

