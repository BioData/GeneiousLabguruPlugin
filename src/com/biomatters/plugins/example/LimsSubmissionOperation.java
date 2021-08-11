package com.biomatters.plugins.example;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.NucleotideSequenceDocument;
import com.biomatters.geneious.publicapi.plugin.*;
import com.biomatters.geneious.publicapi.utilities.StandardIcons;
import jebl.util.ProgressListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import javax.swing.event.DocumentEvent;

import static com.biomatters.plugins.example.LimsConnectorPlugin.LIMS_NAME;

public class LimsSubmissionOperation extends DocumentOperation {

    private final LimsAdapter limsAdapter;

    LimsSubmissionOperation(LimsAdapter limsAdapter) {
        this.limsAdapter = limsAdapter;
    }

    @Override
    public GeneiousActionOptions getActionOptions() {
        return new GeneiousActionOptions("Submit to " + LIMS_NAME, null, StandardIcons.database.getIcons()).setInMainToolbar(true);
    }

    @Override
    public String getHelp() {
        return "Select 1 or more nucleotide sequences to submit them to " + LIMS_NAME;
    }

    @Override
    public DocumentSelectionSignature[] getSelectionSignatures() {
        return new DocumentSelectionSignature[]{
                new DocumentSelectionSignature(NucleotideSequenceDocument.class, 1, Integer.MAX_VALUE)
        };
    }

    @Override
    public List<AnnotatedPluginDocument> performOperation(AnnotatedPluginDocument[] annotatedDocuments, ProgressListener progressListener, Options options, SequenceSelection sequenceSelection) throws DocumentOperationException {
        /*
        First check if which sequences are already in the lims, either they have a LIMS ID on them because they've
        previously been submitted or there's a sequence in the LIMS with exactly the same residues.
        */
        Log my_log = null;
        try {
            my_log = new Log("/Users/Noa/log.txt");
            my_log.logger.setLevel(Level.WARNING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        my_log.logger.warning("sequenceSelection");
        my_log.logger.warning(String.valueOf(sequenceSelection));


        List<String> gb_files = new ArrayList<String>();
        List<AnnotatedPluginDocument> sequencesAlreadyInLims = new ArrayList<>();
        List<AnnotatedPluginDocument> sequencesWithIdsAlready = new ArrayList<>();
        for (AnnotatedPluginDocument annotatedDocument : annotatedDocuments) {
            NucleotideSequenceDocument sequence = (NucleotideSequenceDocument) annotatedDocument.getDocument();
            Optional<String> idOnDocument = LimsAdapter.getIdFromDocumentNotes(annotatedDocument);
            if (idOnDocument.isPresent()) {
                gb_files.add(annotatedDocument.getDocument().toHTML());
                sequencesWithIdsAlready.add(annotatedDocument);
            } else {
                List<String> searchResults = limsAdapter.searchForSequences(sequence.getCharSequence());
                gb_files.add(annotatedDocument.getDocument().toHTML());
                if (!searchResults.isEmpty()) {
                    sequencesAlreadyInLims.add(annotatedDocument);
                }
            }
        }

        //warn about duplicate sequences
        if (!sequencesAlreadyInLims.isEmpty()) {
            if (!Dialogs.showContinueCancelDialog("<html><b>Sequences have been found in " + LIMS_NAME + " that have residues matching these sequences.</b><br><br>" +
                            "New entries will be submitted which may result in duplication of data.</html>",
                    "Matching Sequences Found in " + LIMS_NAME, null, Dialogs.DialogIcon.INFORMATION)) {
                throw new DocumentOperationException.Canceled();
            }
        }

        if (!sequencesWithIdsAlready.isEmpty()) {
            if (!Dialogs.showContinueCancelDialog("<html><b>Some selected sequences already have an ID from " + LIMS_NAME + ".</b><br><br>" +
                            "Submitting will overwrite existing entries in " + LIMS_NAME + ".</html>",
                    "Sequences Already Submitted", null, Dialogs.DialogIcon.WARNING)) {
                throw new DocumentOperationException.Canceled();
            }
        }

        //do submission
        try {
            //find all plasmids names
            List<String> allPlasmidsNames = new ArrayList<String>();
            allPlasmidsNames = findMatches("<cache_name>(.+?)</cache_name>", Arrays.toString(annotatedDocuments));

            for (int i = 0; i < allPlasmidsNames.size(); i++) {
                my_log.logger.warning("gb_files.get(i)");
                my_log.logger.warning(gb_files.get(i));

                List<String> allSequences = new ArrayList<String>();
                allSequences = findMatches("ORIGIN(.+?)//", gb_files.get(i));
                String jsonTemplateString = "{\"token\": \"1619a04b258bbe32caa2b450ed06ae7bb9931adb\", \"item\": {\"title\": \"replace_name\", \"sequence\": \"replace_sequence\"}}";
                String replaceString=jsonTemplateString.replaceAll("replace_sequence", allSequences.get(0));
                String replaceString_1=replaceString.replaceAll("ORIGIN","");
                String replaceString_2=replaceString_1.replaceAll("//","");
                String replaceString_3=replaceString_2.replaceAll("\\R","");
                String replaceString_4=replaceString_3.replaceAll("replace_name",allPlasmidsNames.get(i));

                postRequest("https://my.labguru.com/api/v1/plasmids.json", replaceString_4);
                my_log.logger.warning("jsonBodyStr");
                my_log.logger.warning(replaceString_4);
            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
            my_log.logger.warning(String.valueOf(e));
        } catch (ProtocolException e) {
            e.printStackTrace();
            my_log.logger.warning(String.valueOf(e));
        } catch (IOException e) {
            e.printStackTrace();
            my_log.logger.warning(String.valueOf(e));
        }

        my_log.logger.warning("annotatedDocuments");
        my_log.logger.warning(Arrays.toString(annotatedDocuments));
        limsAdapter.submitSequences(annotatedDocuments);

        Dialogs.DialogOptions dialogOptions = new Dialogs.DialogOptions(Dialogs.OK_ONLY, "Submission Complete", null, Dialogs.DialogIcon.INFORMATION);
        Dialogs.showDialogWithDontShowAgain(dialogOptions, "Sequences submitted successfully", LimsConnectorPlugin.getLimsCode() + "SubmissionComplete", "Don't Show Again");

        return Collections.emptyList();
    }

    @Override
    public boolean isDocumentGenerator() {
        return false;
    }

    @Override
    public Options getOptions(DocumentOperationInput operationInput) throws DocumentOperationException {
        return super.getOptions(operationInput);
    }

    public void postRequest(String urlStr, String jsonBodyStr) throws IOException {
        Log my_log = null;
        try {
            my_log = new Log("/Users/Noa/log.txt");
            my_log.logger.setLevel(Level.WARNING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        URL url = new URL(urlStr);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-Type", "application/json");
        try (OutputStream outputStream = httpURLConnection.getOutputStream()) {
            outputStream.write(jsonBodyStr.getBytes());
            outputStream.flush();
        }
        if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line);
                    my_log.logger.warning("Response");
                    my_log.logger.warning(String.valueOf(response));
                    // ... do something with line
                }
            }
        } else {
            my_log.logger.warning("HttpURLConnection failed");
            // ... do something with unsuccessful response
        }
    }


    public String replaceAll(String regex, String replacement) {
        return Pattern.compile(regex).matcher((CharSequence) this).replaceAll(replacement);
    }

    public static List findMatches(String regex, String text) {
        Log my_log = null;
        try {
            my_log = new Log("/Users/Noa/log.txt");
            my_log.logger.setLevel(Level.WARNING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> allMatches = new ArrayList<String>();
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            my_log.logger.warning("findMatches");
            allMatches.add(matcher.group());
//            my_log.logger.warning(matcher.group());
        }
        return allMatches;
    }

}
