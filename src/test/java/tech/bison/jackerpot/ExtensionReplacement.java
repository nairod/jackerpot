/**
 * File Name: ViewReplacement.java
 * 
 * Copyright (c) 2018 BISON Schweiz AG, All Rights Reserved.
 */

package tech.bison.jackerpot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import tech.bison.jackerpot.source.Replacement;

/**
 *
 * @author dorian.nyffeler
 */
public class ExtensionReplacement implements Replacement {

  private String _oldMethod;
  private String _viewType;
  private String _extensionClass;

  public ExtensionReplacement(String oldMethod, String viewType, String extensionClass) {
    _oldMethod = oldMethod;
    _viewType = viewType;
    _extensionClass = extensionClass;
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
    return "getExtension(" + _extensionClass + ")." + _oldMethod;
  }

  @Override
  public List<String> getNewMethodTypeParameters() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getClassesToImport() {
    return Arrays.asList("tech.bison.jackerpot.testdummies.FooExtension");
  }
}
