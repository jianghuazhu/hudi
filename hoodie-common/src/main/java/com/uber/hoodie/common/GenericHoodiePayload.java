/*
 * Copyright (c) 2016 Uber Technologies, Inc. (hoodie-dev-group@uber.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uber.hoodie.common;

import com.uber.hoodie.avro.MercifulJsonConverter;
import com.uber.hoodie.common.model.HoodieRecordPayload;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class GenericHoodiePayload implements HoodieRecordPayload<GenericHoodiePayload> {
    private byte[] jsonDataCompressed;
    private int dataSize;

    public GenericHoodiePayload(String json) throws IOException {
        this.jsonDataCompressed = compressData(json);
        this.dataSize = json.length();
    }

    @Override public GenericHoodiePayload preCombine(GenericHoodiePayload another) {
        return this;
    }

    @Override public IndexedRecord combineAndGetUpdateValue(IndexedRecord oldRec, Schema schema) throws IOException {
        return getInsertValue(schema);
    }

    @Override public IndexedRecord getInsertValue(Schema schema) throws IOException {
        MercifulJsonConverter jsonConverter = new MercifulJsonConverter(schema);
        return jsonConverter.convert(getJsonData());
    }

    private String getJsonData() throws IOException {
        return unCompressData(jsonDataCompressed);
    }

    private byte[] compressData(String jsonData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        DeflaterOutputStream dos =
            new DeflaterOutputStream(baos, deflater, true);
        try {
            dos.write(jsonData.getBytes());
        } finally {
            dos.flush();
            dos.close();
            // Its important to call this.
            // Deflater takes off-heap native memory and does not release until GC kicks in
            deflater.end();
        }
        return baos.toByteArray();
    }


    private String unCompressData(byte[] data) throws IOException {
        InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(data));
        try {
            StringWriter sw = new StringWriter(dataSize);
            IOUtils.copy(iis, sw);
            return sw.toString();
        } finally {
            iis.close();
        }
    }

}
