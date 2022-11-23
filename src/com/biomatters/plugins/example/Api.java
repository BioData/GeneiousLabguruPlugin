package com.biomatters.plugins.example;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;


public class Api {
    private String url = "https://my.labguru.com/api/v1/plasmids";
    private String authentication_url = "https://my.labguru.com/api/v1/sessions.json";
    private  String upload_file = "https://my.labguru.com/api/v1/attachments.json";

    private static String boundary =  "===" + System.currentTimeMillis() + "===";
    static HttpURLConnection httpConn;
    static DataOutputStream request;
    static final String crlf = "\r\n";
    static final String twoHyphens = "--";

    public String postRequest(String jsonBodyStr) throws IOException {
        URL url = new URL(this.url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-Type", "application/json");

        try (OutputStream outputStream = httpURLConnection.getOutputStream()) {

            outputStream.write(jsonBodyStr.getBytes());
            outputStream.flush();
        }
        if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED || httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line);
                    // ... do something with line
                }
                return response.toString();
//                return String.valueOf(response);
            }
        } else {
            return String.valueOf(httpURLConnection.getResponseCode());
            // ... do something with unsuccessful response
        }
    }

    public String authentication(String jsonBodyStr) throws IOException {
        URL url = new URL(this.authentication_url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-Type", "application/json");

        try (OutputStream outputStream = httpURLConnection.getOutputStream()) {

            outputStream.write(jsonBodyStr.getBytes());
            outputStream.flush();
        }
        if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED || httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line);
                    // ... do something with line
                }
                return response.toString();
//                return String.valueOf(response);
            }
        } else {
            return String.valueOf(httpURLConnection.getResponseCode());
            // ... do something with unsuccessful response
        }
    }

    public String postattachments(File file, String token) throws IOException {

        URL url = new URL(this.upload_file);
        /**
         * This constructor initializes a new HTTP POST request with content type
         * is set to multipart/form-data
         *
         * @param requestURL
         * @throws IOException
         */

//        String filename = file.getPath();
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
        request.writeBytes("Content-Disposition: form-data; name=\"" + "item[title]" + "\""+ crlf);

        request.writeBytes("Content-Type: text/plain; charset=UTF-8" + crlf);
        request.writeBytes(crlf);
        request.writeBytes(uploadFile.getName()+ crlf);
        request.flush();


        request.writeBytes(twoHyphens + boundary + crlf);
        request.writeBytes("Content-Disposition: form-data; name=\"" + "token" + "\""+ crlf);

        request.writeBytes("Content-Type: text/plain; charset=UTF-8" + crlf);
        request.writeBytes(crlf);
        request.writeBytes(token + crlf);
        request.flush();


//        request.writeBytes(twoHyphens + boundary + crlf);
//        request.writeBytes("Content-Disposition: form-data; name=\"" + "item[attach_to_uuid]" + "\""+ crlf);
//
//        request.writeBytes("Content-Type: text/plain; charset=UTF-8" + crlf);
//        request.writeBytes(crlf);
//        request.writeBytes("55623218-9d71-4162-9b20-d17111651531"+ crlf);
//        request.flush();


        String fileName = uploadFile.getName();
        request.writeBytes(twoHyphens + boundary + crlf);
        request.writeBytes("Content-Disposition: form-data; name=\"" +
                "item[attachment]" + "\";filename=\"" +
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
