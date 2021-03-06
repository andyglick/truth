/*
 * Copyright (c) 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.common.truth.extensions.proto;

import static com.google.common.truth.extensions.proto.ProtoSubject.assertThat;
import static org.junit.Assert.fail;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.UnknownFieldSet.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Unit tests for {@link FieldScope}, and their interaction with {@link ProtoSubject}. */
@RunWith(Parameterized.class)
public class FieldScopesTest extends ProtoSubjectTestBase {

  @Parameters(name = "{0}")
  public static Collection<Object[]> parameters() {
    return ProtoSubjectTestBase.parameters();
  }

  // Set up for the ignoringTopLevelField tests.
  // ignoringFieldMessage and ignoringFieldDiffMessage are simple messages with two fields set. They
  // are the same for the "good" field, and different for the "bad" field. The *FieldNumber and
  // *FieldDescriptor members point to these fields.

  private final Message ignoringFieldMessage;
  private final Message ignoringFieldDiffMessage;
  private final int goodFieldNumber;
  private final int badFieldNumber;
  private final FieldDescriptor goodFieldDescriptor;
  private final FieldDescriptor badFieldDescriptor;

  public FieldScopesTest(TestType testType) {
    super(testType);

    ignoringFieldMessage = parse("o_int: 3 r_string: \"foo\"");
    ignoringFieldDiffMessage = parse("o_int: 3 r_string: \"bar\"");
    goodFieldNumber = getFieldNumber("o_int");
    badFieldNumber = getFieldNumber("r_string");
    goodFieldDescriptor = getFieldDescriptor("o_int");
    badFieldDescriptor = getFieldDescriptor("r_string");
  }

  @Test
  public void testUnequalMessages() {
    Message message = parse("o_int: 3 r_string: \"foo\"");
    Message diffMessage = parse("o_int: 5 r_string: \"bar\"");

    expectThat(diffMessage).isNotEqualTo(message);
  }

  @Test
  public void testFieldScopes_all() {
    Message message = parse("o_int: 3 r_string: \"foo\"");
    Message diffMessage = parse("o_int: 5 r_string: \"bar\"");

    expectThat(diffMessage).withPartialScope(FieldScopes.all()).isNotEqualTo(message);
    expectThat(diffMessage).ignoringFieldScope(FieldScopes.all()).isEqualTo(message);

    try {
      assertThat(diffMessage).ignoringFieldScope(FieldScopes.all()).isNotEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsNotEqualToFailed(e);
      expectSubstr(e, "ignored: o_int");
      expectSubstr(e, "ignored: r_string[0]");
    }
  }

  @Test
  public void testFieldScopes_none() {
    Message message = parse("o_int: 3 r_string: \"foo\"");
    Message diffMessage = parse("o_int: 5 r_string: \"bar\"");

    expectThat(diffMessage).ignoringFieldScope(FieldScopes.none()).isNotEqualTo(message);
    expectThat(diffMessage).withPartialScope(FieldScopes.none()).isEqualTo(message);

    try {
      assertThat(diffMessage).withPartialScope(FieldScopes.none()).isNotEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsNotEqualToFailed(e);
      expectSubstr(e, "ignored: o_int");
      expectSubstr(e, "ignored: r_string[0]");
    }
  }

  @Test
  public void testIgnoringTopLevelField_ignoringField() {
    expectThat(ignoringFieldDiffMessage)
        .ignoringField(goodFieldNumber)
        .isNotEqualTo(ignoringFieldMessage);
    expectThat(ignoringFieldDiffMessage)
        .ignoringField(badFieldNumber)
        .isEqualTo(ignoringFieldMessage);

    try {
      assertThat(ignoringFieldDiffMessage)
          .ignoringField(goodFieldNumber)
          .isEqualTo(ignoringFieldMessage);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsEqualToFailed(e);
      expectSubstr(e, "modified: r_string[0]: \"foo\" -> \"bar\"");
    }

    try {
      assertThat(ignoringFieldDiffMessage)
          .ignoringField(badFieldNumber)
          .isNotEqualTo(ignoringFieldMessage);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsNotEqualToFailed(e);
      expectSubstr(e, "ignored: r_string[0]");
    }
  }

  @Test
  public void testIgnoringTopLevelField_fieldScopes_ignoringFields() {
    expectThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.ignoringFields(goodFieldNumber))
        .isNotEqualTo(ignoringFieldMessage);
    expectThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.ignoringFields(goodFieldNumber))
        .isEqualTo(ignoringFieldMessage);
    expectThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.ignoringFields(badFieldNumber))
        .isEqualTo(ignoringFieldMessage);
    expectThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.ignoringFields(badFieldNumber))
        .isNotEqualTo(ignoringFieldMessage);
  }

  @Test
  public void testIgnoringTopLevelField_fieldScopes_allowingFields() {
    expectThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.allowingFields(goodFieldNumber))
        .isEqualTo(ignoringFieldMessage);
    expectThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.allowingFields(goodFieldNumber))
        .isNotEqualTo(ignoringFieldMessage);
    expectThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.allowingFields(badFieldNumber))
        .isNotEqualTo(ignoringFieldMessage);
    expectThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.allowingFields(badFieldNumber))
        .isEqualTo(ignoringFieldMessage);
  }

  @Test
  public void testIgnoringTopLevelField_fieldScopes_allowingFieldDescriptors() {
    expectThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.allowingFieldDescriptors(goodFieldDescriptor))
        .isEqualTo(ignoringFieldMessage);
    expectThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.allowingFieldDescriptors(goodFieldDescriptor))
        .isNotEqualTo(ignoringFieldMessage);
    expectThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.allowingFieldDescriptors(badFieldDescriptor))
        .isNotEqualTo(ignoringFieldMessage);
    expectThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.allowingFieldDescriptors(badFieldDescriptor))
        .isEqualTo(ignoringFieldMessage);
  }

  @Test
  public void testIgnoringTopLevelField_fieldScopes_ignoringFieldDescriptors() {
    expectThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.ignoringFieldDescriptors(goodFieldDescriptor))
        .isNotEqualTo(ignoringFieldMessage);
    expectThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.ignoringFieldDescriptors(goodFieldDescriptor))
        .isEqualTo(ignoringFieldMessage);
    expectThat(ignoringFieldDiffMessage)
        .withPartialScope(FieldScopes.ignoringFieldDescriptors(badFieldDescriptor))
        .isEqualTo(ignoringFieldMessage);
    expectThat(ignoringFieldDiffMessage)
        .ignoringFieldScope(FieldScopes.ignoringFieldDescriptors(badFieldDescriptor))
        .isNotEqualTo(ignoringFieldMessage);
  }

  @Test
  public void testIgnoreSubMessageField() {
    Message message = parse("o_int: 1 o_sub_test_message: { o_int: 2 }");
    Message diffMessage = parse("o_int: 2 o_sub_test_message: { o_int: 2 }");
    Message eqMessage1 = parse("o_int: 1");
    Message eqMessage2 = parse("o_int: 1 o_sub_test_message: {}");
    Message eqMessage3 = parse("o_int: 1 o_sub_test_message: { o_int: 3 r_string: \"x\" }");
    int fieldNumber = getFieldNumber("o_sub_test_message");

    expectThat(diffMessage).ignoringField(fieldNumber).isNotEqualTo(message);
    expectThat(eqMessage1).ignoringField(fieldNumber).isEqualTo(message);
    expectThat(eqMessage2).ignoringField(fieldNumber).isEqualTo(message);
    expectThat(eqMessage3).ignoringField(fieldNumber).isEqualTo(message);

    try {
      assertThat(diffMessage).ignoringField(fieldNumber).isEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsEqualToFailed(e);
      expectSubstr(e, "modified: o_int: 1 -> 2");
    }

    try {
      assertThat(eqMessage3).ignoringField(fieldNumber).isNotEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsNotEqualToFailed(e);
      expectSubstr(e, "ignored: o_sub_test_message");
    }
  }

  @Test
  public void testIgnoringAllButOneFieldOfSubMessage() {
    // Consider all of TestMessage, but none of o_sub_test_message, except
    // o_sub_test_message.o_int.
    Message message =
        parse(
            "o_int: 3 o_sub_test_message: { o_int: 4 r_string: \"foo\" } "
                + "r_sub_test_message: { o_int: 5 r_string: \"bar\" }");

    // All of these differ in a critical field.
    Message diffMessage1 =
        parse(
            "o_int: 999999 o_sub_test_message: { o_int: 4 r_string: \"foo\" } "
                + "r_sub_test_message: { o_int: 5 r_string: \"bar\" }");
    Message diffMessage2 =
        parse(
            "o_int: 3 o_sub_test_message: { o_int: 999999 r_string: \"foo\" } "
                + "r_sub_test_message: { o_int: 5 r_string: \"bar\" }");
    Message diffMessage3 =
        parse(
            "o_int: 3 o_sub_test_message: { o_int: 4 r_string: \"foo\" } "
                + "r_sub_test_message: { o_int: 999999 r_string: \"bar\" }");
    Message diffMessage4 =
        parse(
            "o_int: 3 o_sub_test_message: { o_int: 4 r_string: \"foo\" } "
                + "r_sub_test_message: { o_int: 5 r_string: \"999999\" }");

    // This one only differs in o_sub_test_message.r_string, which is ignored.
    Message eqMessage =
        parse(
            "o_int: 3 o_sub_test_message: { o_int: 4 r_string: \"999999\" } "
                + "r_sub_test_message: { o_int: 5 r_string: \"bar\" }");

    FieldScope fieldScope =
        FieldScopes.ignoringFields(getFieldNumber("o_sub_test_message"))
            .allowingFieldDescriptors(
                getFieldDescriptor("o_sub_test_message").getMessageType().findFieldByName("o_int"));

    expectThat(diffMessage1).withPartialScope(fieldScope).isNotEqualTo(message);
    expectThat(diffMessage2).withPartialScope(fieldScope).isNotEqualTo(message);
    expectThat(diffMessage3).withPartialScope(fieldScope).isNotEqualTo(message);
    expectThat(diffMessage4).withPartialScope(fieldScope).isNotEqualTo(message);
    expectThat(eqMessage).withPartialScope(fieldScope).isEqualTo(message);

    try {
      assertThat(diffMessage4).withPartialScope(fieldScope).isEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsEqualToFailed(e);
      expectSubstr(e, "modified: r_sub_test_message[0].r_string[0]: \"bar\" -> \"999999\"");
    }

    try {
      assertThat(eqMessage).withPartialScope(fieldScope).isNotEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsNotEqualToFailed(e);
      expectSubstr(e, "ignored: o_sub_test_message.r_string[0]");
    }
  }

  @Test
  public void testFromSetFields() {
    Message scopeMessage =
        parse(
            "o_int: 1 r_string: \"x\" o_test_message: { o_int: 1 } "
                + "r_test_message: { r_string: \"x\" } r_test_message: { o_int: 1 } "
                + "o_sub_test_message: { o_test_message: { o_int: 1 } }");

    // 1 = compared, [2, 3] = ignored, 4 = compared and fails
    Message message =
        parse(
            "o_int: 1 r_string: \"1\" o_test_message: {o_int: 1 r_string: \"2\" } "
                + "r_test_message: { o_int: 1 r_string: \"1\" } "
                + "r_test_message: { o_int: 1 r_string: \"1\" } "
                + "o_sub_test_message: { o_int: 2 o_test_message: { o_int: 1 r_string: \"2\" } }");
    Message diffMessage =
        parse(
            "o_int: 4 r_string: \"4\" o_test_message: {o_int: 4 r_string: \"3\" } "
                + "r_test_message: { o_int: 4 r_string: \"4\" } "
                + "r_test_message: { o_int: 4 r_string: \"4\" }"
                + "o_sub_test_message: { r_string: \"3\" o_int: 3 "
                + "o_test_message: { o_int: 4 r_string: \"3\" } }");
    Message eqMessage =
        parse(
            "o_int: 1 r_string: \"1\" o_test_message: {o_int: 1 r_string: \"3\" } "
                + "r_test_message: { o_int: 1 r_string: \"1\" } "
                + "r_test_message: { o_int: 1 r_string: \"1\" }"
                + "o_sub_test_message: { o_int: 3 o_test_message: { o_int: 1 r_string: \"3\" } }");

    expectThat(diffMessage).isNotEqualTo(message);
    expectThat(eqMessage).isNotEqualTo(message);

    expectThat(diffMessage)
        .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
        .isNotEqualTo(message);
    expectThat(eqMessage)
        .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
        .isEqualTo(message);

    try {
      assertThat(diffMessage).isEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsEqualToFailed(e);
      expectSubstr(e, "1 -> 4");
      expectSubstr(e, "\"1\" -> \"4\"");
      expectSubstr(e, "2 -> 3");
      expectSubstr(e, "\"2\" -> \"3\"");
    }

    try {
      assertThat(diffMessage)
          .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
          .isEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsEqualToFailed(e);
      expectSubstr(e, "1 -> 4");
      expectSubstr(e, "\"1\" -> \"4\"");
      expectNoSubstr(e, "2 -> 3");
      expectNoSubstr(e, "\"2\" -> \"3\"");
    }

    try {
      assertThat(eqMessage)
          .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
          .isNotEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsNotEqualToFailed(e);
      expectSubstr(e, "ignored: o_test_message.r_string[0]");
      expectSubstr(e, "ignored: o_sub_test_message.o_int");
      expectSubstr(e, "ignored: o_sub_test_message.o_test_message.r_string[0]");
    }
  }

  @SuppressWarnings("unchecked")
  public void testFromSetFields_unknownFields() {
    if (isProto3()) {
      // No unknown fields in Proto 3.
      return;
    }

    // Make sure that merging of repeated fields, separation by tag number, and separation by
    // unknown field type all work.
    Message scopeMessage =
        newBuilder()
            .setUnknownFields(
                UnknownFieldSet.newBuilder()
                    .addField(20, Field.newBuilder().addFixed32(1).addFixed64(1).build())
                    .addField(
                        21,
                        Field.newBuilder()
                            .addVarint(1)
                            .addLengthDelimited(ByteString.copyFrom("1", StandardCharsets.UTF_8))
                            .addGroup(
                                UnknownFieldSet.newBuilder()
                                    .addField(1, Field.newBuilder().addFixed32(1).build())
                                    .build())
                            .addGroup(
                                UnknownFieldSet.newBuilder()
                                    .addField(2, Field.newBuilder().addFixed64(1).build())
                                    .build())
                            .build())
                    .build())
            .build();

    // 1 = compared, [2, 3] = ignored, 4 = compared and fails
    Message message =
        newBuilder()
            .setUnknownFields(
                UnknownFieldSet.newBuilder()
                    .addField(19, Field.newBuilder().addFixed32(2).addFixed64(2).build())
                    .addField(
                        20,
                        Field.newBuilder()
                            .addFixed32(1)
                            .addFixed64(1)
                            .addVarint(2)
                            .addLengthDelimited(ByteString.copyFrom("2", StandardCharsets.UTF_8))
                            .addGroup(
                                UnknownFieldSet.newBuilder()
                                    .addField(1, Field.newBuilder().addFixed32(2).build())
                                    .build())
                            .build())
                    .addField(
                        21,
                        Field.newBuilder()
                            .addFixed32(2)
                            .addFixed64(2)
                            .addVarint(1)
                            .addLengthDelimited(ByteString.copyFrom("1", StandardCharsets.UTF_8))
                            .addGroup(
                                UnknownFieldSet.newBuilder()
                                    .addField(
                                        1, Field.newBuilder().addFixed32(1).addFixed64(2).build())
                                    .addField(
                                        2, Field.newBuilder().addFixed32(2).addFixed64(1).build())
                                    .addField(3, Field.newBuilder().addFixed32(2).build())
                                    .build())
                            .build())
                    .build())
            .build();
    Message diffMessage =
        newBuilder()
            .setUnknownFields(
                UnknownFieldSet.newBuilder()
                    .addField(19, Field.newBuilder().addFixed32(3).addFixed64(3).build())
                    .addField(
                        20,
                        Field.newBuilder()
                            .addFixed32(4)
                            .addFixed64(4)
                            .addVarint(3)
                            .addLengthDelimited(ByteString.copyFrom("3", StandardCharsets.UTF_8))
                            .addGroup(
                                UnknownFieldSet.newBuilder()
                                    .addField(1, Field.newBuilder().addFixed32(3).build())
                                    .build())
                            .build())
                    .addField(
                        21,
                        Field.newBuilder()
                            .addFixed32(3)
                            .addFixed64(3)
                            .addVarint(4)
                            .addLengthDelimited(ByteString.copyFrom("4", StandardCharsets.UTF_8))
                            .addGroup(
                                UnknownFieldSet.newBuilder()
                                    .addField(
                                        1, Field.newBuilder().addFixed32(4).addFixed64(3).build())
                                    .addField(
                                        2, Field.newBuilder().addFixed32(3).addFixed64(4).build())
                                    .addField(3, Field.newBuilder().addFixed32(3).build())
                                    .build())
                            .build())
                    .build())
            .build();
    Message eqMessage =
        newBuilder()
            .setUnknownFields(
                UnknownFieldSet.newBuilder()
                    .addField(19, Field.newBuilder().addFixed32(3).addFixed64(3).build())
                    .addField(
                        20,
                        Field.newBuilder()
                            .addFixed32(1)
                            .addFixed64(1)
                            .addVarint(3)
                            .addLengthDelimited(ByteString.copyFrom("3", StandardCharsets.UTF_8))
                            .addGroup(
                                UnknownFieldSet.newBuilder()
                                    .addField(1, Field.newBuilder().addFixed32(3).build())
                                    .build())
                            .build())
                    .addField(
                        21,
                        Field.newBuilder()
                            .addFixed32(3)
                            .addFixed64(3)
                            .addVarint(1)
                            .addLengthDelimited(ByteString.copyFrom("1", StandardCharsets.UTF_8))
                            .addGroup(
                                UnknownFieldSet.newBuilder()
                                    .addField(
                                        1, Field.newBuilder().addFixed32(1).addFixed64(3).build())
                                    .addField(
                                        2, Field.newBuilder().addFixed32(3).addFixed64(1).build())
                                    .addField(3, Field.newBuilder().addFixed32(3).build())
                                    .build())
                            .build())
                    .build())
            .build();

    expectThat(diffMessage).isNotEqualTo(message);
    expectThat(eqMessage).isNotEqualTo(message);

    expectThat(diffMessage)
        .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
        .isNotEqualTo(message);
    expectThat(eqMessage)
        .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
        .isEqualTo(message);

    try {
      assertThat(diffMessage).isEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsEqualToFailed(e);
      expectSubstr(e, "1 -> 4");
      expectSubstr(e, "\"1\" -> \"4\"");
      expectSubstr(e, "2 -> 3");
      expectSubstr(e, "\"2\" -> \"3\"");
    }

    try {
      assertThat(diffMessage)
          .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
          .isEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsEqualToFailed(e);
      expectSubstr(e, "1 -> 4");
      expectSubstr(e, "\"1\" -> \"4\"");
      expectNoSubstr(e, "2 -> 3");
      expectNoSubstr(e, "\"2\" -> \"3\"");
    }

    try {
      assertThat(eqMessage)
          .withPartialScope(FieldScopes.fromSetFields(scopeMessage))
          .isNotEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsNotEqualToFailed(e);
      // TODO(user): Add proper checks.
    }
  }

  @Test
  public void testFieldNumbersAreRecursive() {
    // o_int is compared, r_string is not.
    Message message =
        parse("o_int: 1 r_string: \"foo\" r_test_message: { o_int: 2 r_string: \"bar\" }");
    Message diffMessage =
        parse("o_int: 2 r_string: \"bar\" r_test_message: { o_int: 1 r_string: \"foo\" }");
    Message eqMessage =
        parse("o_int: 1 r_string: \"bar\" r_test_message: { o_int: 2 r_string: \"foo\" }");
    int fieldNumber = getFieldNumber("o_int");
    FieldDescriptor fieldDescriptor = getFieldDescriptor("o_int");

    expectThat(diffMessage)
        .withPartialScope(FieldScopes.allowingFields(fieldNumber))
        .isNotEqualTo(message);
    expectThat(eqMessage)
        .withPartialScope(FieldScopes.allowingFields(fieldNumber))
        .isEqualTo(message);
    expectThat(diffMessage)
        .withPartialScope(FieldScopes.allowingFieldDescriptors(fieldDescriptor))
        .isNotEqualTo(message);
    expectThat(eqMessage)
        .withPartialScope(FieldScopes.allowingFieldDescriptors(fieldDescriptor))
        .isEqualTo(message);

    try {
      assertThat(diffMessage)
          .withPartialScope(FieldScopes.allowingFields(fieldNumber))
          .isEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsEqualToFailed(e);
      expectSubstr(e, "modified: o_int: 1 -> 2");
      expectSubstr(e, "modified: r_test_message[0].o_int: 2 -> 1");
    }

    try {
      assertThat(eqMessage)
          .withPartialScope(FieldScopes.allowingFields(fieldNumber))
          .isNotEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsNotEqualToFailed(e);
      expectSubstr(e, "ignored: r_test_message[0].r_string[0]");
    }
  }

  @Test
  public void testMultipleFieldNumbers() {
    Message message = parse("o_int: 1 r_string: \"x\" o_enum: TWO");
    Message diffMessage = parse("o_int: 2 r_string: \"y\" o_enum: TWO");
    Message eqMessage =
        parse("o_int: 1 r_string: \"x\" o_enum: ONE o_sub_test_message: { r_string: \"bar\" }");

    FieldScope fieldScope =
        FieldScopes.allowingFields(getFieldNumber("o_int"), getFieldNumber("r_string"));

    expectThat(diffMessage).withPartialScope(fieldScope).isNotEqualTo(message);
    expectThat(eqMessage).withPartialScope(fieldScope).isEqualTo(message);

    try {
      assertThat(diffMessage).withPartialScope(fieldScope).isEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsEqualToFailed(e);
      expectSubstr(e, "modified: o_int: 1 -> 2");
      expectSubstr(e, "modified: r_string[0]: \"x\" -> \"y\"");
    }

    try {
      assertThat(eqMessage).withPartialScope(fieldScope).isNotEqualTo(message);
      fail("Expected error.");
    } catch (AssertionError e) {
      expectIsNotEqualToFailed(e);
      expectSubstr(e, "ignored: o_enum");
      expectSubstr(e, "ignored: o_sub_test_message");
    }
  }

  @Test
  public void testInvalidFieldNumber() {
    Message message1 = parse("o_int: 44");
    Message message2 = parse("o_int: 33");

    try {
      expectThat(message1).ignoringField(999).isEqualTo(message2);
      fail("Expected error.");
    } catch (IllegalArgumentException e) {
      expectSubstr(e, "Message type " + fullMessageName() + " has no field with number 999.");
    }
  }
}
