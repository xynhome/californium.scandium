/*******************************************************************************
 * Copyright (c) 2014 Bosch Software Innovations GmbH and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Kai Hudalla, Bosch Software Innovations GmbH
 ******************************************************************************/
package org.eclipse.californium.scandium.dtls;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.californium.scandium.dtls.CertificateTypeExtension.CertificateType;
import org.eclipse.californium.scandium.dtls.HelloExtension.ExtensionType;
import org.eclipse.californium.scandium.util.DatagramWriter;
import org.junit.Assert;
import org.junit.Test;

public class HelloExtensionsTest {

	int unsupportedExtensionTypeCode = 0x50;
	byte[] helloExtensionBytes;
	HelloExtensions helloExtensions;
	
	@Test
	public void testSerializationDeserialization() throws HandshakeException {
		ClientCertificateTypeExtension ext = new ClientCertificateTypeExtension(true);
		ext.addCertificateType(CertificateType.X_509);
		ext.addCertificateType(CertificateType.RAW_PUBLIC_KEY);
		
		HelloExtensions extensions = new HelloExtensions();
		extensions.addExtension(ext);
		byte[] serializedExtension = extensions.toByteArray();
		
		HelloExtensions deserializedExt = HelloExtensions.fromByteArray(serializedExtension);
		ClientCertificateTypeExtension certTypeExt = (ClientCertificateTypeExtension)
				deserializedExt.getExtensions().get(0);
		Assert.assertTrue(certTypeExt.getCertificateTypes().size() == 2);
		
	}

	@Test
	public void testFromByteArrayIgnoresUnknownExtensionTypes() throws HandshakeException {
		givenAMixOfSupportedAndUnsupportedHelloExtensions();
		whenDeserializingFromByteArray();
		assertThatSupportedExtensionTypesHaveBeenDeserialized();
	}
	
	private void assertThatSupportedExtensionTypesHaveBeenDeserialized() {
		Assert.assertNotNull(helloExtensions.getExtensions());
		Assert.assertTrue(containsExtensionType(
				ExtensionType.CLIENT_CERT_TYPE.getId(), helloExtensions.getExtensions()));
		Assert.assertTrue(containsExtensionType(
				ExtensionType.SERVER_CERT_TYPE.getId(), helloExtensions.getExtensions()));
		Assert.assertFalse(containsExtensionType(
				unsupportedExtensionTypeCode, helloExtensions.getExtensions()));
	}
	
	private void givenAMixOfSupportedAndUnsupportedHelloExtensions() {
		int length = 0;
		List<byte[]> extensions = new LinkedList<>();
    	// a supported client certificate type extension
		byte[] ext = DtlsTestTools.newClientCertificateTypesExtension(
    			new byte[]{(byte) CertificateType.X_509.getCode()});
		length += ext.length;
    	extensions.add(ext);
    	// extension type 0x50 is not defined by IANA
    	DatagramWriter writer = new DatagramWriter();
    	writer.writeBytes(
    			DtlsTestTools.newHelloExtension(unsupportedExtensionTypeCode, new byte[]{(byte) 0x12}));
    	ext = writer.toByteArray();
		length += ext.length;
    	extensions.add(ext);
    	// a supported server certificate type extension
    	ext = DtlsTestTools.newServerCertificateTypesExtension(
    			new byte[]{(byte) CertificateType.X_509.getCode()});
		length += ext.length;
    	extensions.add(ext);
    	
    	writer = new DatagramWriter();
    	writer.write(length, HelloExtensions.LENGTH_BITS);
    	for (byte[] extension : extensions) {
    		writer.writeBytes(extension);
    	}
    	helloExtensionBytes = writer.toByteArray();
	}
	
	private void whenDeserializingFromByteArray() throws HandshakeException {
		helloExtensions = HelloExtensions.fromByteArray(helloExtensionBytes);
	}
	
    private boolean containsExtensionType(int type, List<HelloExtension> extensions) {
    	for (HelloExtension ext : extensions) {
    		if (ext.getType().getId() == type) {
    			return true;
    		}
    	}
    	return false;
    }
}
