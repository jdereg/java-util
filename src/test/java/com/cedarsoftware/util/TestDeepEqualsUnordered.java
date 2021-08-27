package com.cedarsoftware.util;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertTrue;

public class TestDeepEqualsUnordered
{

  @Test
  public void testUnorderedCollectionWithCollidingHashcodesAndParentLinks() {
    Set<BadHashingValueWithParentLink> elementsA = new HashSet<>();
    elementsA.add(new BadHashingValueWithParentLink(0, 1));
    elementsA.add(new BadHashingValueWithParentLink(1, 0));
    Set<BadHashingValueWithParentLink> elementsB = new HashSet<>();
    elementsB.add(new BadHashingValueWithParentLink(0, 1));
    elementsB.add( new BadHashingValueWithParentLink(1, 0));

    Parent parentA = new Parent();
    parentA.addElements(elementsA);
    Parent parentB = new Parent();
    parentB.addElements(elementsB);

    Map<Object,Object> options = new HashMap<Object, Object>();
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
    private Parent parent;

    public BadHashingValueWithParentLink(int i, int j) {
      this.i = i;
      this.j = j;
    }

    private final int j;

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
