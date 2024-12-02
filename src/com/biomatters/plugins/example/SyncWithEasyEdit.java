package com.biomatters.plugins.example;

import com.biomatters.geneious.publicapi.components.Dialogs;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.sequence.NucleotideSequenceDocument;
import com.biomatters.geneious.publicapi.plugin.*;
import com.biomatters.geneious.publicapi.utilities.FileUtilities;
import com.biomatters.geneious.publicapi.utilities.StandardIcons;
import jebl.util.ProgressListener;

import java.io.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


import static com.biomatters.plugins.example.LimsConnectorPlugin.LIMS_NAME;





//THIS IS WHAT ACTUALLY DOES THE WORK!




public class SyncWithEasyEdit extends DocumentOperation {

    private final LimsAdapter limsAdapter;
    private static File logFile = null;


    SyncWithEasyEdit(LimsAdapter limsAdapter) {
        this.limsAdapter = limsAdapter;
    }

    @Override
    public GeneiousActionOptions getActionOptions() {
        return new GeneiousActionOptions("Submit to " + LIMS_NAME, null, StandardIcons.database.getIcons()).setInMainToolbar(true);
    }
    //    @Override
//    public Options getOptions(AnnotatedPluginDocument... documents) throws DocumentOperationException {
//        Options options_input = new Options(this.getClass());
//        Options.StringOption email = options_input.addStringOption("email", "Email", "Default Email");
//        Options.StringOption password = options_input.addStringOption("password", "Password", "Default Password");
//        Options.StringOption account_id = options_input.addStringOption("account_id", "Account id", "Default account_id");
//        return options_input;
//    }
    @Override
    public Options getOptions(AnnotatedPluginDocument... documents) throws DocumentOperationException {
        return new Options(this.getClass());
    }


    @Override
    public String getHelp() {
        return "Select 1 plasmid to submit to " + LIMS_NAME;
    }

    @Override
    public DocumentSelectionSignature[] getSelectionSignatures() {
        return new DocumentSelectionSignature[]{
                new DocumentSelectionSignature(NucleotideSequenceDocument.class, 1, 1)
        };
    }

    @Override
    public List<AnnotatedPluginDocument> performOperation(AnnotatedPluginDocument[] annotatedDocuments, ProgressListener progressListener, Options options, SequenceSelection sequenceSelection) throws DocumentOperationException {

        for (AnnotatedPluginDocument annotatedDocument : annotatedDocuments) {
            try {
                String plasmid_name = annotatedDocument.getDocument().getName();

                File tempFile = FileUtilities.createTempFile(plasmid_name + "-Geneious_", ".gb", false);
                PluginUtilities.exportDocuments(tempFile, annotatedDocument);
                Path sourcePath = Paths.get(tempFile.getAbsolutePath());
                Path destinationPath = Paths.get(getConfigPath() + "/" + tempFile.getName());
                Log("moving " + plasmid_name + " from " + sourcePath + " to " + destinationPath);
                try {
                    Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    Log("moved successfully");
                    Dialogs.showMessageDialog("File submitted successfully");

                } catch (IOException e) {
                    Log("An error occurred: " + e.getMessage());
                }
            } catch (UnsupportedOperationException e) {
                Log("unsupported format: " + e.getMessage());

            } catch (DocumentOperationException | IOException e) {
                Log("Error occurred: " + e.getMessage());
                System.out.println("Error occurred: " + e.getMessage());
//                                throw new RuntimeException(e);
            }
        }
        return Collections.emptyList();
    }
        //NOA CODE HERE SEARCH
//        List<AnnotatedPluginDocument> sequencesAlreadyInLims = new ArrayList<>();
//        List<AnnotatedPluginDocument> sequencesWithIdsAlready = new ArrayList<>();
//        for (AnnotatedPluginDocument annotatedDocument : annotatedDocuments) {
//
//            NucleotideSequenceDocument sequence = (NucleotideSequenceDocument) annotatedDocument.getDocument();
//            Optional<String> idOnDocument = LimsAdapter.getIdFromDocumentNotes(annotatedDocument);
//            if (idOnDocument.isPresent()) {
//                sequencesWithIdsAlready.add(annotatedDocument);
//            } else {
//                List<String> searchResults = limsAdapter.searchForSequences(sequence.getCharSequence());
//                if (!searchResults.isEmpty()) {
//                    sequencesAlreadyInLims.add(annotatedDocument);
//                }
//            }
//        }
//
//        //warn about duplicate sequences
//        if (!sequencesAlreadyInLims.isEmpty()) {
//            if (!Dialogs.showContinueCancelDialog("<html><b>Sequences have been found in " + LIMS_NAME + " that have residues matching these sequences.</b><br><br>" +
//                            "New entries will be submitted which may result in duplication of data.</html>",
//                    "Matching Sequences Found in " + LIMS_NAME, null, Dialogs.DialogIcon.INFORMATION)) {
//                throw new DocumentOperationException.Canceled();
//            }
//        }
//
//
//        if (!sequencesWithIdsAlready.isEmpty()) {
//            if (!Dialogs.showContinueCancelDialog("<html><b>Some selected sequences already have an ID from " + LIMS_NAME + ".</b><br><br>" +
//                            "Submitting will overwrite existing entries in " + LIMS_NAME + ".</html>",
//                    "Sequences Already Submitted", null, Dialogs.DialogIcon.WARNING)) {
//                throw new DocumentOperationException.Canceled();
//            }
//        }
////
//    }
//
//    public static void checkForDuplicates{
//
//    }


    public static void Log(String content) {
        if (logFile != null && logFile.exists()) {
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(content + "\n");
            } catch (IOException e) {
                System.out.println("An error occurred while writing to the file: " + logFile);
                e.printStackTrace();
            }
        } else {
            System.out.println("Log file not initialized or does not exist.");
        }
    }

    @Override
    public boolean isDocumentGenerator() {
        return false;
    }

    @Override
    public Options getOptions(DocumentOperationInput operationInput) throws DocumentOperationException {
        return super.getOptions(operationInput);
    }


    private static String getConfigPath() { //if easyedit changes config dir name there will be blood
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return System.getenv("APPDATA") + "/EasyEdit2Config/Geneious";  // Returns the Roaming folder
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return System.getProperty("user.home") + "/Library/Application Support/EasyEdit2Config/Geneious";
        } else {
            Log("WEIRD OS:" + System.getProperty("user.home"));
            return "WEIRD OS:" + System.getProperty("user.home");
        }
    }
}


    //return Collections.emptyList();

          /*
        Do the Labguru authentication first
        */

//        String email = options.getValueAsString("email");
//        String password = options.getValueAsString("password");
//        String account_id = options.getValueAsString("account_id");
//
//        JSONObject authentication_payload = new JSONObject();
//        authentication_payload.put("login", email);
//        authentication_payload.put("password", password);
//        authentication_payload.put("account_id", account_id);
//
//        Api authentication_request = new Api();
//        Object authentication_obj= null;
//        try {
//            authentication_obj = JSONValue.parse(authentication_request.authentication(authentication_payload.toString()));
//
//            if (authentication_obj.toString().contains("401")) {
//                Dialogs.DialogOptions dialogOptions = new Dialogs.DialogOptions(Dialogs.OK_ONLY, "Authentication failed ", null, Dialogs.DialogIcon.INFORMATION);
//                Dialogs.showDialogWithDontShowAgain(dialogOptions, "You have entered the wrong password or email address", LimsConnectorPlugin.getLimsCode() + "Authentication failed ", "Don't Show Again");
//                throw new DocumentOperationException.Canceled();
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        /*
//        Check if which sequences are already in the lims, either they have a LIMS ID on them because they've
//        previously been submitted or there's a sequence in the LIMS with exactly the same residues.
//        */
//
//            /*
//            do submission
//            */
//
//            JSONObject authentication_jsonObject = (JSONObject) authentication_obj;
//            String token = (String) authentication_jsonObject.get("token");
//
//        for (AnnotatedPluginDocument annotatedDocument : annotatedDocuments) {
//            try {
//
//                File tempFile = FileUtilities.createTempFile("temp", ".gb", false);
//                PluginUtilities.exportDocuments(tempFile,annotatedDocument);
//
//                File logFile = FileUtilities.createTempFile("log", ".txt", false);
//                FileWriter Writer = new FileWriter(logFile.getAbsolutePath());
//
//
//                String plasmid_name = annotatedDocument.getDocument().getName();
//                String plasmid_sequence = ((NucleotideSequenceDocument) annotatedDocument.getDocument()).getSequenceString();
//                Writer.write("plasmid_name" + plasmid_name);
//
//                Api attachments_request = new Api();
//                JSONObject attachments_obj= null;
//
//                attachments_obj = (JSONObject) JSONValue.parse(attachments_request.postattachments(tempFile, token));
//                String response_attachments = attachments_obj.toString();
//
//                Writer.write("response_attachments " + response_attachments);
//                String gb_attachment_id = "";
//                gb_attachment_id = response_attachments.substring(response_attachments.indexOf("\"id\":") + 5, response_attachments.indexOf("\"height\"") - 1);
//                Writer.write("gb_attachment_id " + gb_attachment_id);
//
//                JSONObject plasmid = new JSONObject();
//                plasmid.put("title", plasmid_name);
//                plasmid.put("sequence", plasmid_sequence);
//                plasmid.put("gb_attachment_id", gb_attachment_id);
//
//                JSONObject json =new JSONObject();
//                json.put("item", plasmid);
//                json.put("token", token);
//
//                Api request = new Api();
//                Object obj= null;
//                obj = JSONValue.parse(request.postRequest(json.toString()));
//                Writer.write("JSONObject plasmid " + obj.toString());
//                Writer.close();
//
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//
//        }
//
//
//        limsAdapter.submitSequences(annotatedDocuments);
//
//        Dialogs.DialogOptions dialogOptions = new Dialogs.DialogOptions(Dialogs.OK_ONLY, "Submission Complete", null, Dialogs.DialogIcon.INFORMATION);
//        Dialogs.showDialogWithDontShowAgain(dialogOptions, "Sequences submitted successfully", LimsConnectorPlugin.getLimsCode() + "SubmissionComplete", "Don't Show Again");
//


