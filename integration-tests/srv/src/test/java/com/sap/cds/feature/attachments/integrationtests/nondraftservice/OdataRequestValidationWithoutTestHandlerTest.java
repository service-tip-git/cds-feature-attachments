package com.sap.cds.feature.attachments.integrationtests.nondraftservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.AttachmentEntity;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;

@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class OdataRequestValidationWithoutTestHandlerTest extends OdataRequestValidationBase {

	@Test
	void serviceHandlerIsNull() {
		assertThat(serviceHandler).isNull();
	}

	@Override
	protected void verifyTwoDeleteEvents(AttachmentEntity itemAttachmentEntityAfterChange, Attachments itemAttachmentAfterChange) {
		// no service handler - nothing to do
	}

	@Override
	protected void verifyNumberOfEvents(String event, int number) {
		// no service handler - nothing to do
	}

	@Override
	protected void verifyDocumentId(Attachments attachmentWithExpectedContent, String attachmentId, String documentId) {
		assertThat(attachmentWithExpectedContent.getDocumentId()).isEqualTo(attachmentId);
	}

	@Override
	protected void verifyContentAndDocumentId(Attachments attachment, String testContent, Attachments itemAttachment) throws IOException {
		assertThat(attachment.getContent().readAllBytes()).isEqualTo(testContent.getBytes(StandardCharsets.UTF_8));
		assertThat(attachment.getDocumentId()).isEqualTo(itemAttachment.getId());
	}

	@Override
	protected void verifyContentAndDocumentIdForAttachmentEntity(AttachmentEntity attachment, String testContent, AttachmentEntity itemAttachment) throws IOException {
		assertThat(attachment.getContent().readAllBytes()).isEqualTo(testContent.getBytes(StandardCharsets.UTF_8));
		assertThat(attachment.getDocumentId()).isEqualTo(itemAttachment.getId());
	}

	@Override
	protected void clearServiceHandlerContext() {
		//no service handler - nothing to do
	}

	@Override
	protected void clearServiceHandlerDocuments() {
		//no service handler - nothing to do
	}

	@Override
	protected void verifySingleCreateEvent(String documentId, String content) {
		//no service handler - nothing to do
	}

	@Override
	protected void verifySingleDeletionEvent(String documentId) {
		//no service handler - nothing to do
	}

	@Override
	protected void verifySingleReadEvent(String documentId) {
		//no service handler - nothing to do
	}

	@Override
	protected void verifyNoAttachmentEventsCalled() {
		//no service handler - nothing to do
	}

	@Override
	protected void verifyEventContextEmptyForEvent(String... events) {
		//no service handler - nothing to do
	}

}
