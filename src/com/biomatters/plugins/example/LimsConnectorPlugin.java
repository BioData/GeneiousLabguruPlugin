package com.biomatters.plugins.example;

import com.biomatters.geneious.publicapi.plugin.DocumentOperation;
import com.biomatters.geneious.publicapi.plugin.GeneiousPlugin;
import com.biomatters.geneious.publicapi.plugin.GeneiousService;

import java.io.File;
@SuppressWarnings("unused")
public class LimsConnectorPlugin extends GeneiousPlugin {

    static final String LIMS_NAME = "Labguru"; //todo
    private static final String PLUGIN_VERSION = "0.0.5"; //todo

    private LimsAdapter limsAdapter;

    /**
     * @return a string for use in programmatic identifiers
     */
    static String getLimsCode() {
        return LIMS_NAME.replace(" ", "");
    }

    @Override
    public String getName() {
        return LIMS_NAME + " Connector ";
    }

    @Override
    public String getDescription() {
        return "Allows submission of plasmids to/from " + LIMS_NAME;
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
        return PLUGIN_VERSION;
    }

    @Override
    public String getMinimumApiVersion() {
        return "1.00";
    }

    @Override
    public int getMaximumApiVersion() {
        return 4;
    }

    @Override
    public String getEmailAddressForCrashes() {
        return "support@biodata.com";
    }

    @Override
    public void initialize(File pluginUserDirectory, File pluginDirectory) {
        limsAdapter = new InMemoryLimsAdapter(); //todo implement LimsAdapter for your system and return it here. Be sure to read the javadoc on LimsAdapter methods carefully to make sure you fulfill their contracts
    }

    @Override
    public GeneiousService[] getServices() {
        return new GeneiousService[]{
                new LimsDatabaseService(limsAdapter)
        };
    }

    @Override
    public DocumentOperation[] getDocumentOperations() {
        return new DocumentOperation[]{
                new LimsSubmissionOperation(limsAdapter)
        };
    }
}