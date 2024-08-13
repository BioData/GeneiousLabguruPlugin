package com.biomatters.plugins.example;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.NucleotideSequenceDocument;
import com.biomatters.geneious.publicapi.plugin.*;
import com.biomatters.geneious.publicapi.plugin.Options;
import com.biomatters.geneious.publicapi.utilities.StandardIcons;
import com.google.gson.*;
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
    public Options getOptions(AnnotatedPluginDocument... documents) throws DocumentOperationException {
        Options options_input = new Options(this.getClass());
        Options.StringOption email = options_input.addStringOption("email", "Email", "Default Email");
        Options.StringOption password = options_input.addStringOption("password", "Password", "Default Password");
        Log my_log = null;
        try {
            my_log = new Log("/Users/Noa/log.txt");
        } catch (IOException e) {
            my_log.logger.warning("IOException");
            my_log.logger.warning(String.valueOf(e));
            e.printStackTrace();
        }
        my_log.logger.setLevel(Level.WARNING);
        my_log.logger.warning("options");
        my_log.logger.warning(email.getValue());
        my_log.logger.warning(password.getValue());

        return options_input;
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

        List<String> gb_files = new ArrayList<String>();
        List<String> plasmidsNames = new ArrayList<String>();
        List<AnnotatedPluginDocument> sequencesAlreadyInLims = new ArrayList<>();
        List<AnnotatedPluginDocument> sequencesWithIdsAlready = new ArrayList<>();
        for (AnnotatedPluginDocument annotatedDocument : annotatedDocuments) {
            NucleotideSequenceDocument sequence = (NucleotideSequenceDocument) annotatedDocument.getDocument();
            Optional<String> idOnDocument = LimsAdapter.getIdFromDocumentNotes(annotatedDocument);
            if (idOnDocument.isPresent()) {
                gb_files.add(annotatedDocument.getDocument().toHTML());
                plasmidsNames.add(annotatedDocument.getDocument().getName());
                sequencesWithIdsAlready.add(annotatedDocument);
            } else {
                List<String> searchResults = limsAdapter.searchForSequences(sequence.getCharSequence());
                gb_files.add(annotatedDocument.getDocument().toHTML());
                plasmidsNames.add(annotatedDocument.getDocument().getName());
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
            //options_input
            my_log.logger.warning("options_input");
            Options options_input = new Options(getClass());
            Options.BooleanOption reverseComplement = options_input.addBooleanOption("reverseComplement", "Reverse Complement", false);
            Options.StringOption name = options_input.addStringOption("name", "Name", "Default Name");
            my_log.logger.warning("options_input2");

            if (Dialogs.showOptionsDialog(options_input,"Title",true)) { // returns true if user clicked OK.
                my_log.logger.warning("options_input3");
                if (reverseComplement.getValue()) {
                    System.out.println("Reverse complement selected and name="+name.getValue());
                }
            }

        }catch (Exception e) {
            e.printStackTrace();
            my_log.logger.warning(String.valueOf(e));
        }

        try{
            my_log.logger.warning("options_input4");


            //find all plasmids names
            List<String> allPlasmidsNames = new ArrayList<String>();
            allPlasmidsNames = findMatches("<cache_name>(.+?)</cache_name>", Arrays.toString(annotatedDocuments));

            for (int i = 0; i < plasmidsNames.size(); i++) {
                my_log.logger.warning("gb_files.get(i)");
                my_log.logger.warning(plasmidsNames.get(i));

                User user = new User("noakap92@gmail.com", "203641931Noa");
                Gson gsonUsr = new Gson();
                String userJson = gsonUsr.toJson(user);
                String response = postRequest("https://my.labguru.com/api/v1/sessions.json", userJson);

                my_log.logger.warning("response token");
                my_log.logger.warning("response");
                my_log.logger.warning(response);


                List<String> allSequences = new ArrayList<String>();
                allSequences = findMatches("ORIGIN(.+?)//", gb_files.get(i));
                String replaceString=allSequences.get(0).replaceAll("ORIGIN","");
                String replaceString_1=replaceString.replaceAll("//","");
                String replaceString_2=replaceString_1.replaceAll("\\R","");

                Plasmid plasmid = new Plasmid(plasmidsNames.get(i), replaceString_2);
                Item item = new Item(plasmid);
                Gson gson = new Gson();
                String token = "501d54b546976e851f3a9c3d6f0580d043cdb103";
                JsonElement jsonElement = gson.toJsonTree(item);
                jsonElement.getAsJsonObject().addProperty("token", token);
                String itemJson = gson.toJson(jsonElement);

                postRequest("https://my.labguru.com/api/v1/plasmids.json", itemJson);
                my_log.logger.warning("jsonBodyStr");
                my_log.logger.warning(itemJson);

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
        } catch (Exception e) {
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

    public String postRequest(String urlStr, String jsonBodyStr) throws IOException {
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
        if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED || httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line);
                    my_log.logger.warning("Response");
                    my_log.logger.warning(String.valueOf(response));
                    // ... do something with line
                }
                return String.valueOf(response);
            }
        } else {
            my_log.logger.warning("HttpURLConnection failed");
            my_log.logger.warning(String.valueOf(httpURLConnection.getResponseCode()));
            return String.valueOf(httpURLConnection.getResponseCode());
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
            my_log.logger.warning(matcher.group());
        }
        return allMatches;
    }

}
