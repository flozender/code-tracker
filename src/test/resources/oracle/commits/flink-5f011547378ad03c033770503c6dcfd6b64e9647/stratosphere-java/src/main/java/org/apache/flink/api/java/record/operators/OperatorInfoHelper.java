/***********************************************************************************************************************
 * Copyright (C) 2010-2013 by the Apache Flink project (http://flink.incubator.apache.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 **********************************************************************************************************************/

package org.apache.flink.api.java.record.operators;

import org.apache.flink.api.common.operators.BinaryOperatorInformation;
import org.apache.flink.api.common.operators.OperatorInformation;
import org.apache.flink.api.common.operators.UnaryOperatorInformation;
import org.apache.flink.api.java.typeutils.RecordTypeInfo;
import org.apache.flink.types.Record;

public class OperatorInfoHelper {

	public static OperatorInformation<Record> source() {
		return new OperatorInformation<Record>(new RecordTypeInfo());
	}

	public static UnaryOperatorInformation<Record, Record> unary() {
		return new UnaryOperatorInformation<Record, Record>(new RecordTypeInfo(), new RecordTypeInfo());
	}

	public static BinaryOperatorInformation<Record, Record, Record> binary() {
		return new BinaryOperatorInformation<Record, Record, Record>(new RecordTypeInfo(), new RecordTypeInfo(), new RecordTypeInfo());
	}
}
