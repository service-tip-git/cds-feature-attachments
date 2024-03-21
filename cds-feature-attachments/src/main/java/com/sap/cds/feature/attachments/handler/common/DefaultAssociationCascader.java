package com.sap.cds.feature.attachments.handler.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.ql.cqn.CqnReference.Segment;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.reflect.CdsStructuredType;

public class DefaultAssociationCascader implements AssociationCascader {

	@Override
	public List<LinkedList<AssociationIdentifier>> findEntityPath(CdsModel model, CdsEntity entity) {
		var firstList = new LinkedList<AssociationIdentifier>();
		var internalResultList = getAttachmentAssociationPath(model, entity, "", firstList, new ArrayList<>(List.of(entity.getQualifiedName())));

		return new ArrayList<>(internalResultList);
	}

	//TODO refactor and harmonize with ReadAttachmentsHandler
	private List<LinkedList<AssociationIdentifier>> getAttachmentAssociationPath(CdsModel model, CdsEntity entity, String associationName, LinkedList<AssociationIdentifier> firstList, List<String> processedEntities) {
		var internalResultList = new ArrayList<LinkedList<AssociationIdentifier>>();
		var currentList = new AtomicReference<LinkedList<AssociationIdentifier>>();
		var localProcessEntities = new ArrayList<String>();
		currentList.set(new LinkedList<>());

		var query = entity.query();
		var entityName = query.flatMap(cqnSelect -> cqnSelect.from().asRef().segments().stream().map(Segment::id).findFirst()).orElseGet(() -> entity.getQualifiedName());

		var baseEntity = model.findEntity(entityName).orElseThrow();
		var isMediaEntity = isMediaEntity(baseEntity);
		if (isMediaEntity) {
			var identifier = new AssociationIdentifier(associationName, entity.getQualifiedName(), isMediaEntity);
			firstList.addLast(identifier);
		}

		if (isMediaEntity) {
			internalResultList.add(firstList);
			return internalResultList;
		}

		Map<String, CdsEntity> associations = entity.elements().filter(element -> element.getType().isAssociation() && element.getType().as(CdsAssociationType.class).isComposition()).collect(Collectors.toMap(CdsElementDefinition::getName, element -> element.getType().as(CdsAssociationType.class).getTarget()));

		if (associations.isEmpty()) {
			return internalResultList;
		}

		var newListNeeded = false;
		for (var associatedElement : associations.entrySet()) {
			if (!processedEntities.contains(associatedElement.getValue().getQualifiedName())) {
				if (newListNeeded) {
					currentList.set(new LinkedList<>());
					currentList.get().addAll(firstList);
					processedEntities = localProcessEntities;
				} else {
					firstList.add(new AssociationIdentifier(associationName, entity.getQualifiedName(), false));
					currentList.get().addAll(firstList);
					localProcessEntities = new ArrayList<>(processedEntities);
				}
				processedEntities.add(associatedElement.getValue().getQualifiedName());
				newListNeeded = true;
				var result = getAttachmentAssociationPath(model, associatedElement.getValue(), associatedElement.getKey(), currentList.get(), processedEntities);
				internalResultList.addAll(result);
			}
		}

		return internalResultList;
	}

	private boolean isMediaEntity(CdsStructuredType baseEntity) {
		return baseEntity.getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false);
	}

}
