package com.tyron.builder.project.api;

import androidx.annotation.NonNull;
import java.io.File;
import java.util.List;
import java.util.Map;

public interface AndroidModule extends JavaModule, KotlinModule {

  /**
   * @return The directory where android resource xml files are searched
   */
  File getAndroidResourcesDirectory();

  File getNativeLibrariesDirectory();

  File getAssetsDirectory();

  String getNameSpace();

  String getNameSpace(File file);

  String getApplicationId();

  String getApplicationId(File file);

  File getManifestFile();

  int getTargetSdk();

  int getMinSdk();

  int getVersionCode();

  String getVersionName();

  boolean getViewBindingEnabled();

  boolean getViewBindingEnabled(File file);

  boolean getMinifyEnabled();

  boolean getMinifyEnabled(File file);

  boolean getZipAlignEnabled();

  boolean getUseLegacyPackaging();

  List<String> getExcludes();

  /** Return a map of fully qualified name and the file object of an R.java class */
  Map<String, File> getResourceClasses();

  void addResourceClass(@NonNull File file);
}
