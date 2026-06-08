package com.hti.mapper;
 
import com.hti.entity.EntityMetadata;
import com.hti.response.Entitymetadataresponse;
 
public class Entitymetadatamapper {
 
    private Entitymetadatamapper() {}
 
    public static Entitymetadataresponse toResponse(EntityMetadata entityMetadata) {
        return Entitymetadataresponse.builder()
                .id(entityMetadata.getId())
                .organisationId(entityMetadata.getOrganisationId())
                .entityId(entityMetadata.getEntityId())
                .metadata(entityMetadata.getMetadata())
                .createdAt(entityMetadata.getCreatedAt())
                .updatedAt(entityMetadata.getUpdatedAt())
                .build();
    }
}
