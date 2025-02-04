package com.tyron.builder.project.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.cache.CacheHolder.CacheKey;
import com.tyron.common.util.StringSearch;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jetbrains.kotlin.com.intellij.util.ReflectionUtil;

public class AndroidModuleImpl extends JavaModuleImpl implements AndroidModule {

  private final Map<String, File> mKotlinFiles;
  private Map<String, File> mResourceClasses;

  public AndroidModuleImpl(File root) {
    super(root);

    mKotlinFiles = new HashMap<>();
    mResourceClasses = new HashMap<>(1);
  }

  @Override
  public void open() throws IOException {
    super.open();
    File gradle = getGradleFile();
    if (!gradle.exists()) {
      throw new IOException("Unable to open build.gradle file");
    }
  }

  @Override
  public void index() {
    super.index();

    Consumer<File> kotlinConsumer = this::addKotlinFile;

    if (getJavaDirectory().exists()) {
      FileUtils.iterateFiles(
              getJavaDirectory(), FileFilterUtils.suffixFileFilter(".kt"), TrueFileFilter.INSTANCE)
          .forEachRemaining(kotlinConsumer);
    }

    if (getKotlinDirectory().exists()) {
      FileUtils.iterateFiles(
              getKotlinDirectory(),
              FileFilterUtils.suffixFileFilter(".kt"),
              TrueFileFilter.INSTANCE)
          .forEachRemaining(kotlinConsumer);
    }

    // R.java files
    //        File gen = new File(getBuildDirectory(), "gen");
    //        if (gen.exists()) {
    //            FileUtils.iterateFiles(gen,
    //                    FileFilterUtils.suffixFileFilter(".java"),
    //                    TrueFileFilter.INSTANCE
    //            ).forEachRemaining(this::addJavaFile);
    //        }
  }

  @Override
  public File getAndroidResourcesDirectory() {
    File custom = getPathSetting("android_resources_directory");
    if (custom.exists()) {
      return custom;
    }
    return new File(getRootFile(), "src/main/res");
  }

  @Override
  public Set<String> getAllClasses() {
    Set<String> classes = super.getAllClasses();
    classes.addAll(mKotlinFiles.keySet());
    return classes;
  }

  @Override
  public File getNativeLibrariesDirectory() {
    File custom = getPathSetting("native_libraries_directory");
    if (custom.exists()) {
      return custom;
    }
    return new File(getRootFile(), "src/main/jniLibs");
  }

  @Override
  public File getAssetsDirectory() {
    File custom = getPathSetting("assets_directory");
    if (custom.exists()) {
      return custom;
    }
    return new File(getRootFile(), "src/main/assets");
  }

  @Override
  public String getNameSpace() {
    return parseNameSpace(getGradleFile());
  }

  private String parseNameSpace(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseNameSpace(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return null;
  }

  @Override
  public String getApplicationId() {
    return parseApplicationId(getGradleFile());
  }

  private String parseApplicationId(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseApplicationId(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return null;
  }

  public String parseNameSpace(String readString) throws IOException {
    Pattern NAMESPLACE = Pattern.compile("\\s*(namespace)\\s*([\"'])([a-zA-Z0-9._'/\\\\:-]+)\\2");
    readString = readString.replaceAll("\\s*//.*", "");
    Matcher matcher = NAMESPLACE.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String namespace = String.valueOf(declaration);
        if (namespace != null && !namespace.isEmpty()) {
          return namespace;
        }
      }
    }
    return null;
  }

  public String parseApplicationId(String readString) throws IOException {
    Pattern APPLICATION_ID = Pattern.compile("\\s*(applicationId)\\s*(')([a-zA-Z0-9.'/-:\\-]+)(')");
    Pattern APPLICATION_ID_QUOT =
        Pattern.compile("\\s*(applicationId)\\s*(\")([a-zA-Z0-9.'/-:\\-]+)(\")");

    readString = readString.replaceAll("\\s*//.*", "");

    Matcher matcher = APPLICATION_ID.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String applicationId = String.valueOf(declaration);
        if (applicationId != null && !applicationId.isEmpty()) {
          return applicationId;
        }
      }
    }
    matcher = APPLICATION_ID_QUOT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null) {
        String applicationId = String.valueOf(declaration);
        if (applicationId != null && !applicationId.isEmpty()) {
          return applicationId;
        }
      }
    }
    return null;
  }

  @Override
  public String getNameSpace(File gradle) {
    return parseNameSpace(gradle);
  }

  @Override
  public String getApplicationId(File gradle) {
    return parseApplicationId(gradle);
  }

  @Override
  public boolean getViewBindingEnabled() {
    return parseViewBindingEnabled(getGradleFile());
  }

  @Override
  public boolean getViewBindingEnabled(File file) {
    return parseViewBindingEnabled(file);
  }

  private boolean parseViewBindingEnabled(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseViewBindingEnabled(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return false;
  }

  private boolean parseViewBindingEnabled(String readString) throws IOException {
    Pattern VIEW_BINDING_ENABLED =
        Pattern.compile("\\s*(viewBinding)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Matcher matcher = VIEW_BINDING_ENABLED.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        boolean viewBindingEnabled = Boolean.parseBoolean(String.valueOf(declaration));
        if (viewBindingEnabled) {
          return true;
        } else {
          return false;
        }
      }
    }
    return false;
  }

  @Override
  public File getManifestFile() {
    File custom = getPathSetting("android_manifest_file");
    if (custom.exists()) {
      return custom;
    }
    return new File(getRootFile(), "src/main/AndroidManifest.xml");
  }

  @Override
  public int getTargetSdk() {
    return parseTargetSdk(getGradleFile());
  }

  @Override
  public int getTargetSdk(File gradle) {
    return parseTargetSdk(gradle);
  }

  private int parseTargetSdk(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseTargetSdk(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return 33;
  }

  public static int parseTargetSdk(String readString) throws IOException {
    Pattern TARGET_SDK = Pattern.compile("\\s*(targetSdk)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Pattern TARGET_SDK_VERSION =
        Pattern.compile("\\s*(targetSdkVersion)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");

    Matcher matcher = TARGET_SDK.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null & !declaration.isEmpty()) {

        try {
          int targetSdk = Integer.parseInt(String.valueOf(declaration));
          return targetSdk;
        } catch (NumberFormatException e) {
          // Handle the exception here, such as logging an error or returning a default value
          e.printStackTrace();
        }
      }
    }
    matcher = TARGET_SDK_VERSION.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null & !declaration.isEmpty()) {

        try {
          int targetSdk = Integer.parseInt(String.valueOf(declaration));
          return targetSdk;
        } catch (NumberFormatException e) {
          // Handle the exception here, such as logging an error or returning a default value
          e.printStackTrace();
        }
      }
    }
    return 33;
  }

  @Override
  public int getMinSdk() {
    return parseMinSdk(getGradleFile());
  }

  @Override
  public int getMinSdk(File gradle) {
    return parseMinSdk(gradle);
  }

  private int parseMinSdk(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseMinSdk(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return 21;
  }

  public static int parseMinSdk(String readString) throws IOException {
    Pattern MIN_SDK = Pattern.compile("\\s*(minSdk)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Pattern MIN_SDK_VERSION = Pattern.compile("\\s*(minSdkVersion)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Matcher matcher = MIN_SDK.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null & !declaration.isEmpty()) {
        try {
          int minSdk = Integer.parseInt(String.valueOf(declaration));
          return minSdk;
        } catch (NumberFormatException e) {
          // Handle the exception here, such as logging an error or returning a default value
          e.printStackTrace();
        }
      }
    }
    matcher = MIN_SDK_VERSION.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null & !declaration.isEmpty()) {
        try {
          int minSdk = Integer.parseInt(String.valueOf(declaration));
          return minSdk;
        } catch (NumberFormatException e) {
          // Handle the exception here, such as logging an error or returning a default value
          e.printStackTrace();
        }
      }
    }
    return 21;
  }

  @Override
  public int getVersionCode() {
    return parseVersionCode(getGradleFile());
  }

  @Override
  public int getVersionCode(File gradle) {
    return parseVersionCode(gradle);
  }

  private int parseVersionCode(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseVersionCode(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return 1;
  }

  public static int parseVersionCode(String readString) throws IOException {
    Pattern VERSION_CODE = Pattern.compile("\\s*(versionCode)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Matcher matcher = VERSION_CODE.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null & !declaration.isEmpty()) {
        try {
          int minSdk = Integer.parseInt(String.valueOf(declaration));
          return minSdk;
        } catch (NumberFormatException e) {
          // Handle the exception here, such as logging an error or returning a default value
          e.printStackTrace();
        }
      }
    }
    return 1;
  }

  @Override
  public String getVersionName() {
    return parseVersionName(getGradleFile());
  }

  @Override
  public String getVersionName(File gradle) {
    return parseVersionName(gradle);
  }

  private String parseVersionName(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseVersionName(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return "1.0";
  }

  public static String parseVersionName(String readString) throws IOException {
    Pattern VERSION_NAME = Pattern.compile("\\s*(versionName)\\s*(')([a-zA-Z0-9.'/-:\\-]+)(')");
    Pattern VERSION_NAME_QUOT =
        Pattern.compile("\\s*(versionName)\\s*(\")([a-zA-Z0-9.'/-:\\-]+)(\")");

    readString = readString.replaceAll("\\s*//.*", "");
    Matcher matcher = VERSION_NAME.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        String versionName = String.valueOf(declaration);
        if (versionName != null && !versionName.isEmpty()) {
          return versionName;
        }
      }
    }
    matcher = VERSION_NAME_QUOT.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        String versionName = String.valueOf(declaration);
        if (versionName != null && !versionName.isEmpty()) {
          return versionName;
        }
      }
    }
    return "1.0";
  }

  @Override
  public boolean getMinifyEnabled() {
    return parseMinifyEnabled(getGradleFile());
  }

  @Override
  public boolean getMinifyEnabled(File file) {
    return parseMinifyEnabled(file);
  }

  private boolean parseMinifyEnabled(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseMinifyEnabled(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return false;
  }

  private boolean parseMinifyEnabled(String readString) throws IOException {
    Pattern MINIFY_ENABLED = Pattern.compile("\\s*(minifyEnabled)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Matcher matcher = MINIFY_ENABLED.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        boolean minifyEnabled = Boolean.parseBoolean(String.valueOf(declaration));
        if (minifyEnabled) {
          return true;
        } else {
          return false;
        }
      }
    }
    return false;
  }

  @Override
  public boolean getZipAlignEnabled() {
    return parseZipAlignEnabled(getGradleFile());
  }

  private boolean parseZipAlignEnabled(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseZipAlignEnabled(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return false;
  }

  private boolean parseZipAlignEnabled(String readString) throws IOException {
    Pattern ZIP_ALIGN_ENABLED =
        Pattern.compile("\\s*(zipAlignEnabled)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Matcher matcher = ZIP_ALIGN_ENABLED.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        boolean zipAlignEnabled = Boolean.parseBoolean(String.valueOf(declaration));
        if (zipAlignEnabled) {
          return true;
        } else {
          return false;
        }
      }
    }
    return false;
  }

  @Override
  public boolean getUseLegacyPackaging() {
    return parseUseLegacyPackaging(getGradleFile());
  }

  private boolean parseUseLegacyPackaging(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseUseLegacyPackaging(readString);
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return false;
  }

  private boolean parseUseLegacyPackaging(String readString) throws IOException {
    Pattern USE_LEGACY_PACKAGING =
        Pattern.compile("\\s*(useLegacyPackaging)\\s*()([a-zA-Z0-9.'/-:\\-]+)()");
    Matcher matcher = USE_LEGACY_PACKAGING.matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        boolean useLegacyPackaging = Boolean.parseBoolean(String.valueOf(declaration));
        if (useLegacyPackaging) {
          return true;
        } else {
          return false;
        }
      }
    }
    return false;
  }

  @Override
  public List<String> getExcludes() {
    return parseExcludes(getGradleFile());
  }

  private List<String> parseExcludes(File gradle) {
    if (gradle != null && gradle.exists()) {

      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        return parseExcludes(readString);
      } catch (IOException e) {
      }
    }
    return null;
  }

  private List<String> parseExcludes(String readString) throws IOException {
    List<String> excludes = new ArrayList<>();
    Matcher matcher =
        Pattern.compile("resources\\.excludes\\s*\\+?=\\s*\\[(.*)\\]").matcher(readString);
    if (matcher.find()) {
      String[] exclusions = matcher.group(1).split(",");
      for (String declaration : exclusions) {
        if (declaration != null && !declaration.isEmpty()) {
          excludes.add(declaration.trim());
        }
      }
    }

    // Match file exclusions
    matcher =
        Pattern.compile("(exclude|exclude\\s+group:)\\s+[\"']([^\"']*)[\"']").matcher(readString);
    while (matcher.find()) {
      String declaration = matcher.group(3);
      if (declaration != null && !declaration.isEmpty()) {
        excludes.add(declaration);
      }
    }
    return excludes;
  }

  @Override
  public void addResourceClass(@NonNull File file) {
    if (!file.getName().endsWith(".java")) {
      return;
    }
    String packageName = StringSearch.packageName(file);
    String className;
    if (packageName == null) {
      className = file.getName().replace(".java", "");
    } else {
      className = packageName + "." + file.getName().replace(".java", "");
    }
    mResourceClasses.put(className, file);
  }

  @Override
  public Map<String, File> getResourceClasses() {
    return ImmutableMap.copyOf(mResourceClasses);
  }

  @NonNull
  @Override
  public Map<String, File> getKotlinFiles() {
    return ImmutableMap.copyOf(mKotlinFiles);
  }

  @NonNull
  @Override
  public File getKotlinDirectory() {
    File custom = getPathSetting("kotlin_directory");
    if (custom.exists()) {
      return custom;
    }
    return new File(getRootFile(), "src/main/kotlin");
  }

  @Nullable
  @Override
  public File getKotlinFile(String packageName) {
    return mKotlinFiles.get(packageName);
  }

  @Override
  public void addKotlinFile(File file) {
    String packageName = StringSearch.packageName(file);
    if (packageName == null) {
      packageName = "";
    }
    String fqn = packageName + "." + file.getName().replace(".kt", "");
    mKotlinFiles.put(fqn, file);
  }

  @Override
  public void clear() {
    super.clear();

    try {
      Class<?> clazz = Class.forName("com.tyron.builder.compiler.symbol.MergeSymbolsTask");
      removeCache(ReflectionUtil.getStaticFieldValue(clazz, CacheKey.class, "CACHE_KEY"));
    } catch (Throwable e) {
      throw new Error(e);
    }
  }
}
