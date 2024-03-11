package com.sap.cds.feature.attachments.handler.applicationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Items;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Roots;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Roots_;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.Attachment;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.runtime.CdsRuntime;

class DeleteAttachmentsHandlerTest {

	private static CdsRuntime runtime;

	private DeleteAttachmentsHandler cut;
	private AttachmentsReader attachmentsReader;
	private ModifyAttachmentEvent modifyAttachmentEvent;
	private CdsDeleteEventContext context;

	@BeforeAll
	static void classSetup() {
		runtime = RuntimeHelper.runtime;
	}

	@BeforeEach
	void setup() {
		attachmentsReader = mock(AttachmentsReader.class);
		modifyAttachmentEvent = mock(ModifyAttachmentEvent.class);
		cut = new DeleteAttachmentsHandler(attachmentsReader, modifyAttachmentEvent);

		context = mock(CdsDeleteEventContext.class);
	}

	@Test
	void noAttachmentDataServiceNotCalled() {
		var entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
		when(context.getTarget()).thenReturn(entity);
		when(context.getModel()).thenReturn(runtime.getCdsModel());

		cut.processBefore(context);

		verifyNoInteractions(modifyAttachmentEvent);
	}

	@Test
	void attachmentDataExistsServiceIsCalled() {
		var entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
		when(context.getTarget()).thenReturn(entity);
		when(context.getModel()).thenReturn(runtime.getCdsModel());
		var data = Attachment.create();
		data.setId("test");
		data.setDocumentId("test");
		var inputStream = mock(InputStream.class);
		data.setContent(inputStream);
		when(attachmentsReader.readAttachments(context.getModel(), context.getTarget(), context.getCqn())).thenReturn(List.of(data));

		cut.processBefore(context);

		verify(modifyAttachmentEvent).processEvent(any(), eq(entity.getElement(Attachment.CONTENT)), eq(inputStream), eq(data), eq(Map.of("ID", data.getId())));
		assertThat(data.getContent()).isNull();
	}

	@Test
	void attachmentDataExistsAsExpandServiceIsCalled() {
		var rootEntity = runtime.getCdsModel().findEntity(Roots_.CDS_NAME).orElseThrow();
		var entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
		when(context.getTarget()).thenReturn(rootEntity);
		when(context.getModel()).thenReturn(runtime.getCdsModel());
		var inputStream = mock(InputStream.class);
		var attachment1 = buildAttachment("id1", inputStream);
		var attachment2 = buildAttachment(UUID.randomUUID().toString(), inputStream);
		var root = Roots.create();
		var items = Items.create();
		root.setItemTable(List.of(items));
		items.setAttachments(List.of(attachment1, attachment2));
		when(attachmentsReader.readAttachments(context.getModel(), context.getTarget(), context.getCqn())).thenReturn(List.of(root));

		cut.processBefore(context);

		verify(modifyAttachmentEvent).processEvent(any(Path.class), any(), eq(inputStream), eq(attachment1), eq(Map.of("ID", attachment1.getId())));
		verify(modifyAttachmentEvent).processEvent(any(Path.class), any(), eq(inputStream), eq(attachment2), eq(Map.of("ID", attachment2.getId())));
		assertThat(attachment1.getContent()).isNull();
		assertThat(attachment2.getContent()).isNull();
	}

	@Test
	void classHasCorrectAnnotation() {
		var deleteHandlerAnnotation = cut.getClass().getAnnotation(ServiceName.class);

		assertThat(deleteHandlerAnnotation.type()).containsOnly(ApplicationService.class);
		assertThat(deleteHandlerAnnotation.value()).containsOnly("*");
	}

	@Test
	void methodHasCorrectAnnotations() throws NoSuchMethodException {
		var method = cut.getClass().getMethod("processBefore", CdsDeleteEventContext.class);

		var deleteBeforeAnnotation = method.getAnnotation(Before.class);
		var deleteHandlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

		assertThat(deleteBeforeAnnotation.event()).containsOnly(CqnService.EVENT_DELETE);
		assertThat(deleteHandlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
	}

	private Attachment buildAttachment(String id, InputStream inputStream) {
		var attachment = Attachment.create();
		attachment.setId(id);
		attachment.setDocumentId("doc_" + id);
		attachment.setContent(inputStream);
		return attachment;
	}


}
