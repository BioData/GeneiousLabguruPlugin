package com.biomatters.plugins.example;

import com.biomatters.geneious.publicapi.databaseservice.*;
import com.biomatters.geneious.publicapi.documents.AnnotatedPluginDocument;
import com.biomatters.geneious.publicapi.documents.PluginDocument;
import com.biomatters.geneious.publicapi.documents.URN;
import com.biomatters.geneious.publicapi.documents.sequence.NucleotideSequenceDocument;
import com.biomatters.geneious.publicapi.plugin.*;
import com.biomatters.geneious.publicapi.utilities.FileUtilities;
import jebl.util.ProgressListener;
import org.apache.commons.logging.LogFactory;
import org.virion.jam.util.SimpleListener;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static com.biomatters.geneious.publicapi.utilities.StandardIcons.database;
import static com.biomatters.geneious.publicapi.utilities.StandardIcons.document;

public class LimsListenerPlugin extends GeneiousPlugin {
    private static final org.apache.commons.logging.Log log = LogFactory.getLog(LimsListenerPlugin.class);
    private static File logFile = null;
    private static FileWriter Writer = null;


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

    ArrayList<String> startFiles = new ArrayList<>();

    public static final String LGFolder = "Labguru Sync Folder";
    public static boolean firstTime = true;

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

    public static void deleteFolder(Path path) throws IOException {
        // Try with resources to ensure proper closure of resources
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    deleteFolder(entry);
                } else {
                    Files.delete(entry);
                }
            }
            Files.delete(path);
        }
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
            try {
                WritableDatabaseService folder = db.createChildFolder(LGFolder);
            } catch (DatabaseServiceException e) {
                throw new RuntimeException(e);
            }

            WritableDatabaseService registry = db.getChildService(LGFolder);


            Path destDir = Paths.get(getConfigPath());
                                if (destDir.toFile().exists()) {
                                    Log("deleting existing dest dir: " + destDir);
                                    try {
                                        deleteFolder(destDir);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    Log("deleted successfully");
                                }
                                destDir.toFile().mkdirs();
                                Log("Destination directory created: " + destDir);
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

                        public void runFirstTime(AnnotatedPluginDocument annotatedDocument) throws DatabaseServiceException {
                            Log("running first time");
                            List<AnnotatedPluginDocument> documentsInSameFolder = annotatedDocument.getDatabase().retrieve(Query.Factory.createBrowseQuery(), ProgressListener.EMPTY);
                            for (AnnotatedPluginDocument s : documentsInSameFolder){
                                String name = s.getName();
                                Log("adding " + name);
                                startFiles.add(name);
                            }
                            Log("finished first time");
                            firstTime = false;
                        }
                        @Override
                        protected void _add(AnnotatedPluginDocument annotatedDocument, Map<String, Object> searchResultProperties) {

                            try {
                                if (logFile == null) { //TODO: put in first time?
                                    logFile = FileUtilities.createTempFile("log_listener", ".txt", false);
                                }
                                String plasmid_name = annotatedDocument.getDocument().getName();
                                if (firstTime) {
                                    runFirstTime(annotatedDocument);
                                    return;
                                }
                                if (startFiles.contains(plasmid_name)) return;

                                Log("Plasmid name: " + plasmid_name + "\n" + "LogFile: " + logFile);
//
                                File tempFile = FileUtilities.createTempFile(plasmid_name + "-Geneious_", ".gb", false);
                                PluginUtilities.exportDocuments(tempFile,annotatedDocument);
                                Path sourcePath = Paths.get(tempFile.getAbsolutePath());
                                Path destinationPath = Paths.get(getConfigPath()+ "/" + tempFile.getName());
                                    Log("moving " + plasmid_name + " from " + sourcePath + " to " + destinationPath);
                                    try {
                                        Files.move(sourcePath, destinationPath  , StandardCopyOption.REPLACE_EXISTING);
                                        Log("moved successfully");
                                    } catch (IOException e) {
                                        Log("An error occurred: " + e.getMessage());
                                    }
                            } catch (DocumentOperationException | IOException | DatabaseServiceException e){
                                Log("Error occurred: " + e.getMessage());
                                System.out.println("Error occurred: " + e.getMessage());
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void remove(AnnotatedPluginDocument annotatedDocument) {

//TODO BRING BACK?
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
