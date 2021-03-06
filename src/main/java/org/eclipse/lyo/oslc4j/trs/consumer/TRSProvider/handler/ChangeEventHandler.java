/**
 * Copyright (c) 2016-2017   KTH Royal Institute of Technology.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 * Omar Kacimi         -  Initial implementation
 * Andrew Berezovskyi  -  Lyo contribution updates
 */
package org.eclipse.lyo.oslc4j.trs.consumer.TRSProvider.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.jena.rdf.model.Model;
import org.apache.log4j.Logger;
import org.eclipse.lyo.core.trs.ChangeEvent;
import org.eclipse.lyo.core.trs.Deletion;
import org.eclipse.lyo.oslc4j.trs.consumer.concurrent.TRSTaskHandler;
import org.eclipse.lyo.oslc4j.trs.consumer.httpclient.TRSHttpClient;
import org.eclipse.lyo.oslc4j.trs.consumer.sparql.SparqlUtil;

import net.oauth.OAuthException;

/**
 * Class for handling the creation of the sparql update for processing a change
 * event
 *
 * @author Omar
 *
 */
public class ChangeEventHandler extends TRSTaskHandler {

    final static Logger logger = Logger.getLogger(ChangeEventHandler.class);
    /**
     * The change event to be processed
     */
    ChangeEvent handledChangeEvent;
    /**
     * A list of updates passed as an argument to which the sparql update for
     * processing the change event will be added
     */
    List<String> queries;
    /**
     * the size of the rdf representation of the rdf member
     */
    AtomicLong modelSize;

    public ChangeEventHandler(TRSHttpClient oslcClient, String sparqlQueryService, String sparqlUpdateService,
            String baseAuth_userName, String baseAuth_pwd, ChangeEvent handledChangeEvent, List<String> queries,
            AtomicLong modelSize) {
        super(oslcClient, sparqlQueryService, sparqlUpdateService, baseAuth_userName, baseAuth_pwd);
        this.handledChangeEvent = handledChangeEvent;
        threadName = "Change Event for resource: " + handledChangeEvent.getChanged() + " handler thread";
        this.queries = queries;
        this.modelSize = modelSize;
    }

    private void processChangeEvent() throws IOException, OAuthException, URISyntaxException {
        URI changed = handledChangeEvent.getChanged();
        logger.debug("creating query for resource " + changed.toString() + " change event ");
        String query = "";
        if (handledChangeEvent instanceof Deletion) {
            query = SparqlUtil.getChangeEventQuery(handledChangeEvent, null);
            queries.add(query.toString());
        } else {

            Model updatedResRepresentation = (Model) fetchTRSRemoteResource(changed.toString(), Model.class);
            if (updatedResRepresentation != null) {
                modelSize.set(modelSize.get() + updatedResRepresentation.size());
                query = SparqlUtil.getChangeEventQuery(handledChangeEvent, updatedResRepresentation);
                queries.add(query.toString());
            } else {
                logger.error("could not retrieve representation of member resource with uri: " + changed.toString());
            }
        }

        logger.info("finished creating query for resource " + changed.toString() + " change event ");
    }

    @Override
    protected void processTRSTask() {
        try {
            super.processTRSTask();
            processChangeEvent();
        } catch (IOException e) {

            logger.error(e);
        } catch (OAuthException e) {

            logger.error(e);
        } catch (URISyntaxException e) {

            logger.error(e);
        }
    }

}
