package com.biomatters.plugins.example;

import com.biomatters.geneious.publicapi.databaseservice.*;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.PluginDocument;
import com.biomatters.geneious.publicapi.documents.URN;
import com.biomatters.geneious.publicapi.documents.sequence.NucleotideSequenceDocument;
import com.biomatters.geneious.publicapi.plugin.*;
import com.biomatters.geneious.publicapi.utilities.FileUtilities;
import org.virion.jam.util.SimpleListener;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class LimsListenerPlugin extends GeneiousPlugin {

    @Override
    public String getName() {
        return "Labguru Listener";
    }

    @Override
    public String getDescription() {
        return "Allows submission of plasmids to Labguru";
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public String getAuthors() {
        return "Labguru";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public int getMaximumApiVersion() {
        return 4;
    }

    @Override
    public String getMinimumApiVersion() {
        return "4.201900";
    }

    @Override
    public void initialize(File pluginUserDirectory, File pluginDirectory) {
        PluginUtilities.addWritableDatabaseServiceRootListener(new PluginUtilities.WritableDatabaseServicesListener() {
            @Override
            public void serviceAdded(WritableDatabaseService service) {
                refreshDatabases();
            }

            @Override
            public void serviceRemoved(WritableDatabaseService service) {
                refreshDatabases();
            }
        });
        refreshDatabases();
    }

    private void refreshDatabases() {
        for (WritableDatabaseService db : PluginUtilities.getWritableDatabaseServiceRoots()) {
            WritableDatabaseService registry = db.getChildService("Labguru Sync Folder");
            if (registry != null) {
                try {
                    registry.retrieve(Query.Factory.createBrowseQuery(), new RetrieveCallback() {
                        @Override
                        protected void _add(PluginDocument document, Map<String, Object> searchResultProperties) {
                            //todo sync to LIMS
                        }

                        @Override
                        public boolean acceptsChangesAfterRetrieveCompletes(SimpleListener noLongerWantsChangesListener) {
                            return true;
                        }

                        @Override
                        protected void _add(AnnotatedPluginDocument annotatedDocument, Map<String, Object> searchResultProperties) {
                            // sync to Labguru
                            try{

                                File logFile = FileUtilities.createTempFile("log_listener", ".txt", false);
                                FileWriter Writer = new FileWriter(logFile.getAbsolutePath());


                                String plasmid_name = annotatedDocument.getDocument().getName();
                                Writer.write("plasmid_name " + plasmid_name);

                                File tempFile = FileUtilities.createTempFile("temp", ".gb", false);
                                PluginUtilities.exportDocuments(tempFile,annotatedDocument);


                                Api attachments_request = new Api();
                                JSONObject attachments_obj= null;

                                attachments_obj = (JSONObject) JSONValue.parse(attachments_request.postattachments(tempFile, plasmid_name, false));
                                String response_attachments = attachments_obj.toString();

                                Writer.write("response " + response_attachments);

                                Writer.close();


                            } catch (DocumentOperationException e) {
                                throw new RuntimeException(e);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                        }

                        @Override
                        public void remove(AnnotatedPluginDocument annotatedDocument) {

                            try{

                                File logFile = FileUtilities.createTempFile("log", ".txt", false);
                                FileWriter Writer = new FileWriter(logFile.getAbsolutePath());


                                String plasmid_name = annotatedDocument.getDocument().getName();
                                String plasmid_sequence = ((NucleotideSequenceDocument) annotatedDocument.getDocument()).getSequenceString();
                                Writer.write("plasmid_name " + plasmid_name);


                                File tempFile = FileUtilities.createTempFile("temp", ".gb", false);
                                PluginUtilities.exportDocuments(tempFile,annotatedDocument);

                                Api attachments_request = new Api();
                                JSONObject attachments_obj= null;

                                attachments_obj = (JSONObject) JSONValue.parse(attachments_request.postattachments(tempFile, plasmid_name, true));
                                String response_attachments = attachments_obj.toString();

                                Writer.write("response " + response_attachments);

                                Writer.close();

                            } catch (DocumentOperationException e) {
                                throw new RuntimeException(e);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }


                            //todo sync to LIMS
                        }
                    }, new URN[0]);
                } catch (DatabaseServiceException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
