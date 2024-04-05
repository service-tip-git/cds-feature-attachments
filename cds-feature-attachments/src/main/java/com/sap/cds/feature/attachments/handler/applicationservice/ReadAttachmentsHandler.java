package com.sap.cds.feature.attachments.handler.applicationservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Generator;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.applicationevents.model.LazyProxyInputStream;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.applicationevents.modifier.ItemModifierProvider;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.draftservice.constants.DraftConstants;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.CQL;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

/**
	* The class {@link ReadAttachmentsHandler} is an event handler that is
	* responsible for reading attachments for entities.
	* In the before read event, it modifies the CQN to include the document ID and status.
	* In the after read event, it adds a proxy for the stream of the attachments service to the data.
	* Only if the data are read the proxy forwards the request to the attachment service to read the attachment.
	* This is needed to have a filled stream in the data to enable the OData V4 adapter to enrich the data that
	* a link to the document can be shown on the UI.
	*/
@ServiceName(value = "*", type = ApplicationService.class)
public class ReadAttachmentsHandler implements EventHandler {

	private final AttachmentService attachmentService;
	private final ItemModifierProvider provider;

	public ReadAttachmentsHandler(AttachmentService attachmentService, ItemModifierProvider provider) {
		this.attachmentService = attachmentService;
		this.provider = provider;
	}

	@Before(event = CqnService.EVENT_READ)
	@HandlerOrder(HandlerOrder.EARLY)
	public void processBefore(CdsReadEventContext context) {
		var cdsModel = context.getModel();
		var fieldNames = getAttachmentAssociations(cdsModel, context.getTarget(), "", new ArrayList<>());
		if (!fieldNames.isEmpty()) {
			var resultCqn = CQL.copy(context.getCqn(), provider.getBeforeReadDocumentIdEnhancer(fieldNames));
			context.setCqn(resultCqn);
		}
	}

	@After(event = CqnService.EVENT_READ)
	@HandlerOrder(HandlerOrder.EARLY)
	public void processAfter(CdsReadEventContext context, List<CdsData> data) {
		if (ApplicationHandlerHelper.isContentFieldInData(context.getTarget(), data)) {
			Filter filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();
			Generator generator = (path, element, isNull) -> {
				if (path.target().values().containsKey(element.getName())) {
					var documentId = (String) path.target().values().get(Attachments.DOCUMENT_ID);
					var status = (String) path.target().values().get(Attachments.STATUS_CODE);
					if (Objects.nonNull(documentId)) {
						return new LazyProxyInputStream(() -> attachmentService.readAttachment(documentId), status);
					}
				}
				return null;
			};

			CdsDataProcessor.create().addGenerator(filter, generator).process(data, context.getTarget());
		}
	}

	private List<String> getAttachmentAssociations(CdsModel model, CdsEntity entity, String associationName, List<String> processedEntities) {
		var associationNames = new ArrayList<String>();
		var baseEntity = ApplicationHandlerHelper.getBaseEntity(model, entity);
		if (ApplicationHandlerHelper.isMediaEntity(baseEntity)) {
			associationNames.add(associationName);
		}

		Map<String, CdsEntity> annotatedEntitiesMap = entity.elements().filter(element -> element.getType().isAssociation())
																																																		.collect(Collectors.toMap(CdsElementDefinition::getName, element -> element.getType()
																																																																																																																								.as(CdsAssociationType.class)
																																																																																																																								.getTarget()));

		if (annotatedEntitiesMap.isEmpty()) {
			return associationNames;
		}

		for (var associatedElement : annotatedEntitiesMap.entrySet()) {
			if (!associationNames.contains(associatedElement.getKey()) && !processedEntities.contains(associatedElement.getKey()) && !DraftConstants.SIBLING_ENTITY.equals(associatedElement.getKey())) {
				processedEntities.add(associatedElement.getKey());
				var result = getAttachmentAssociations(model, associatedElement.getValue(), associatedElement.getKey(), processedEntities);
				associationNames.addAll(result);
			}
		}
		return associationNames;
	}

}
