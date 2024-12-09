package com.cedarsoftware.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeepEqualsUnorderedTest
{
  @Test
  public void testUnorderedCollectionWithCollidingHashcodesAndParentLinks()
  {
    Set<BadHashingValueWithParentLink> elementsA = new HashSet<>();
    elementsA.add(new BadHashingValueWithParentLink(0, 1));
    elementsA.add(new BadHashingValueWithParentLink(1, 0));
    Set<BadHashingValueWithParentLink> elementsB = new HashSet<>();
    elementsB.add(new BadHashingValueWithParentLink(0, 1));
    elementsB.add(new BadHashingValueWithParentLink(1, 0));

    Parent parentA = new Parent();
    parentA.addElements(elementsA);
    Parent parentB = new Parent();
    parentB.addElements(elementsB);

    Map<String, Object> options = new HashMap<>();
    options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
    assertTrue(DeepEquals.deepEquals(parentA, parentB, options));
  }


  private static class Parent {

    private final Set<BadHashingValueWithParentLink> elements = new HashSet<>();

    public Parent() {
    }

    public void addElement(BadHashingValueWithParentLink element){
      element.setParent(this);
      elements.add(element);
    }


    public void addElements(Set<BadHashingValueWithParentLink> a) {
      a.forEach(this::addElement);
    }
  }
  private static class BadHashingValueWithParentLink {
    private final int i;
    private final int j;
    private Parent parent;

    public BadHashingValueWithParentLink(int i, int j) {
      this.i = i;
      this.j = j;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      BadHashingValueWithParentLink that = (BadHashingValueWithParentLink) o;
      return i == that.i && j == that.j;
    }

    @Override
    public int hashCode() {
      return i+j;
    }


    public void setParent(Parent configuration) {
      parent = configuration;
    }
  }

}
