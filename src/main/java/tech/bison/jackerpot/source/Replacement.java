/***File Name:Replacement.java**Copyright(c)2018 BISON Schweiz AG,All Rights Reserved.*/

package tech.bison.jackerpot.source;

import java.util.List;

/***

@author dorian.nyffeler
 */
 public interface Replacement {
 String getOldMethod();

 String getFullQualifiedViewType();

 String getNewMethod();

 List<String> getNewMethodTypeParameters();

 List<String> getClassesToImport();
 }
