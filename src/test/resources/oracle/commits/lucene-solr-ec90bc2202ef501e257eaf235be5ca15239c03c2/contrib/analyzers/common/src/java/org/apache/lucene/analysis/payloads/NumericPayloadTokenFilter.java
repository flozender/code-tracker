package org.apache.lucene.analysis.payloads;
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.index.Payload;

import java.io.IOException;


/**
 * Assigns a payload to a token based on the {@link org.apache.lucene.analysis.Token#type()}
 *
 **/
public class NumericPayloadTokenFilter extends TokenFilter {

  private String typeMatch;
  private Payload thePayload;

  private PayloadAttribute payloadAtt;
  private TypeAttribute typeAtt;

  public NumericPayloadTokenFilter(TokenStream input, float payload, String typeMatch) {
    super(input);
    //Need to encode the payload
    thePayload = new Payload(PayloadHelper.encodeFloat(payload));
    this.typeMatch = typeMatch;
    payloadAtt = addAttribute(PayloadAttribute.class);
    typeAtt = addAttribute(TypeAttribute.class);
  }

  public final boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      if (typeAtt.type().equals(typeMatch))
        payloadAtt.setPayload(thePayload);
      return true;
    } else {
      return false;
    }
  }

  /** @deprecated Will be removed in Lucene 3.0. This method is final, as it should
   * not be overridden. Delegates to the backwards compatibility layer. */
  public final Token next(final Token reusableToken) throws java.io.IOException {
    return super.next(reusableToken);
  }

  /** @deprecated Will be removed in Lucene 3.0. This method is final, as it should
   * not be overridden. Delegates to the backwards compatibility layer. */
  public final Token next() throws java.io.IOException {
    return super.next();
  }
}
