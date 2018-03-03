/**
 * File Name: ViewReplacement.java
 * 
 * Copyright (c) 2018 BISON Schweiz AG, All Rights Reserved.
 */

package tech.bison.jackerpot;

import java.util.Arrays;
import java.util.List;

import tech.bison.jackerpot.source.Replacement;

/**
 * TD2:dorian.nyffeler Auto-generated comment for class
 *
 * @author dorian.nyffeler
 */
public class ViewReplacement implements Replacement {

  private String _oldMethod;
  private String _viewType;
  private String _newMethod;
  private String _newMethodTypeParameter;

  public ViewReplacement(String oldMethod, String viewType, String newMethod, String newMethodTypeParameter) {
    _oldMethod = oldMethod;
    _viewType = viewType;
    _newMethod = newMethod;
    _newMethodTypeParameter = newMethodTypeParameter;
  }

  @Override
  public String getOldMethod() {
    return _oldMethod;
  }

  @Override
  public String getFullQualifiedViewType() {
    return _viewType;
  }

  @Override
  public String getNewMethod() {
    return _newMethod;
  }

  @Override
  public List<String> getNewMethodTypeParameters() {
    return Arrays.asList(_newMethodTypeParameter);
  }

  @Override
  public List<String> getClassesToImport() {
    return Arrays.asList("tech.bison.jackerpot.testdummies.IBarView");
  }
}
