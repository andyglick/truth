/*
 * Copyright (c) 2014 Google, Inc.
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
package com.google.common.truth;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.PrimitiveLongArraySubject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link PrimitiveLongArraySubject}.
 *
 * @author Christian Gruber (cgruber@israfil.net)
 */
@RunWith(JUnit4.class)
public class PrimitiveLongArraySubjectTest {
  @Test
  public void isEqualTo() {
    assertThat(array(2L, 5)).isEqualTo(array(2L, 5));
  }

  @Test
  public void isEqualTo_Same() {
    long[] same = array(2L, 5);
    assertThat(same).isEqualTo(same);
  }

  @Test
  public void asList() {
    assertThat(array(5, 2, 9)).asList().containsAllOf(2L, 9L);
  }

  @Test
  public void isEqualTo_Fail_UnequalOrdering() {
    try {
      assertThat(array(2, 3)).isEqualTo(array(3, 2));
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <(long[]) [2, 3]> is equal to <[3, 2]>");
    }
  }

  @Test
  public void isEqualTo_Fail_NotAnArray() {
    try {
      assertThat(array(2, 3, 4)).isEqualTo(new int[] {});
    } catch (AssertionError e) {
      assertThat(e.getMessage())
          .contains("Incompatible types compared. expected: int[], actual: long[]");
    }
  }

  @Test
  public void isNotEqualTo_SameLengths() {
    assertThat(array(2, 3)).isNotEqualTo(array(3, 2));
  }

  @Test
  public void isNotEqualTo_DifferentLengths() {
    assertThat(array(2, 3)).isNotEqualTo(array(2, 3, 1));
  }

  @Test
  public void isNotEqualTo_DifferentTypes() {
    assertThat(array(2, 3)).isNotEqualTo(new Object());
  }

  @Test
  public void isNotEqualTo_FailEquals() {
    try {
      assertThat(array(2, 3)).isNotEqualTo(array(2, 3));
    } catch (AssertionError e) {
      assertThat(e).hasMessage("<(long[]) [2, 3]> unexpectedly equal to [2, 3].");
    }
  }

  @Test
  public void isNotEqualTo_FailSame() {
    try {
      long[] same = array(2, 3);
      assertThat(same).isNotEqualTo(same);
    } catch (AssertionError e) {
      assertThat(e).hasMessage("<(long[]) [2, 3]> unexpectedly equal to [2, 3].");
    }
  }

  private static long[] array(long... ts) {
    return ts;
  }
}
