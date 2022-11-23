package com.biomatters.plugins.example;

import com.biomatters.geneious.publicapi.databaseservice.*;
import com.biomatters.geneious.publicapi.documents.URN;
import com.biomatters.geneious.publicapi.plugin.Icons;
import com.biomatters.geneious.publicapi.utilities.StandardIcons;

public class LimsDatabaseService extends DatabaseService {

    private final LimsAdapter limsAdapter;

    LimsDatabaseService(LimsAdapter limsAdapter) {
        this.limsAdapter = limsAdapter;
    }

    public QueryField[] getSearchFields() {
        return limsAdapter.getSearchFields();
    }

    @Override
    public void retrieve(Query query, RetrieveCallback retrieveCallback, URN[] urnsToNotRetrieve) throws DatabaseServiceException {
        limsAdapter.retrieve(query, retrieveCallback);
    }

    @Override
    public String getUniqueID() {
        return LimsConnectorPlugin.getLimsCode();
    }

    @Override
    public String getName() {
        return LimsConnectorPlugin.LIMS_NAME;
    }

    @Override
    public String getDescription() {
        return "Access sequences in " + LimsConnectorPlugin.LIMS_NAME;
    }

    @Override
    public String getHelp() {
        return "";
    }

    @Override
    public Icons getIcons() {
        return StandardIcons.database.getIcons();
    }
}
