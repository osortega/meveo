package org.meveo.api.rest.custom.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.jboss.logging.Logger;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.PersistenceDto;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.elresolver.ELException;
import org.meveo.interfaces.Entity;
import org.meveo.interfaces.EntityOrRelation;
import org.meveo.interfaces.EntityRelation;
import org.meveo.persistence.neo4j.service.Neo4jService;
import org.meveo.persistence.scheduler.AtomicPersistencePlan;
import org.meveo.persistence.scheduler.CyclicDependencyException;
import org.meveo.persistence.scheduler.OrderedPersistenceService;
import org.meveo.persistence.scheduler.SchedulingService;

/**
 * @author Edward P. Legaspi <czetsuya@gmail.com>
 * @lastModifiedVersion 6.4.0
 */
@Path("/neo4j/persist")
@Api("Neo4j persistence")
public class Neo4JPersistenceRs {

    protected static final Logger LOGGER = Logger.getLogger(Neo4JPersistenceRs.class);

    @Inject
    protected SchedulingService schedulingService;

    @Inject
    protected OrderedPersistenceService<Neo4jService> scheduledPersistenceService;

    @Inject
    protected Neo4jService neo4jService;

    @QueryParam("neo4jConfiguration")
    private String neo4jConfiguration;

    @DELETE
    @ApiOperation(value="Delete data to be persisted")
    public Response delete(@ApiParam("Data to be persisted") Collection<PersistenceDto> dtos) throws BusinessException {

        for (PersistenceDto persistenceDto : dtos) {
            if (persistenceDto.getDiscriminator().equals(EntityOrRelation.ENTITY)) {
                neo4jService.deleteEntity(neo4jConfiguration, persistenceDto.getType(), persistenceDto.getProperties());
            }
        }

        return Response.noContent().build();
    }

    @POST
    @Path("/entities")
    @ApiOperation(value="Persist entity data to be persisted")
    public Response persistEntities(@ApiParam("Data to be persisted") Collection<PersistenceDto> dtos) throws CyclicDependencyException, ELException, EntityDoesNotExistsException, IOException, BusinessApiException, BusinessException {

        /* Extract the entities */
        final List<Entity> entities = dtos.stream()
                .filter(persistenceDto -> persistenceDto.getDiscriminator().equals(EntityOrRelation.ENTITY))
                .map(persistenceDto -> new Entity.Builder()
                        .type(persistenceDto.getType())
                        .name(persistenceDto.getName())
                        .properties(persistenceDto.getProperties())
                        .build())
                        .collect(Collectors.toList());

        /* Extract the relationships */
        final List<EntityRelation> relations = dtos.stream()
                .filter(persistenceDto -> persistenceDto.getDiscriminator().equals(EntityOrRelation.RELATION))
                .map(persistenceDto -> {
                    final Optional<Entity> source = entities.stream()
                            .filter(entity -> entity.getName().equals(persistenceDto.getSource()))
                            .findAny();
                    final Optional<Entity> target = entities.stream().filter(entity -> entity.getName().equals(persistenceDto.getTarget()))
                            .findAny();
                    if (source.isPresent() && target.isPresent()) {
                        return new EntityRelation.Builder()
                                .type(persistenceDto.getType())
                                .source(source.get())
                                .target(target.get())
                                .properties(persistenceDto.getProperties())
                                .build();
                    }
                    LOGGER.warnv("Relationship of type {} between {} and {} will not be persisted because of missing source or target", persistenceDto.getType(), persistenceDto.getSource(), persistenceDto.getTarget());
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        /* Create the persistence schedule */
        List<EntityOrRelation> entityOrRelations = new ArrayList<>(entities);
        entityOrRelations.addAll(relations);
        AtomicPersistencePlan atomicPersistencePlan = schedulingService.schedule(entityOrRelations);

        /* Persist the entities and return 201 created response */
        scheduledPersistenceService.persist(neo4jConfiguration, atomicPersistencePlan);
        return Response.status(201).build();

    }
    
}
