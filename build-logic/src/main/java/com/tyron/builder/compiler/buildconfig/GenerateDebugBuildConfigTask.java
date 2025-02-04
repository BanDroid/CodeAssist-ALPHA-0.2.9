package com.tyron.builder.compiler.buildconfig;

import android.util.Log;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;

public class GenerateDebugBuildConfigTask extends Task<AndroidModule> {

  private static final String TAG = "generateDebugBuildConfig";

  public GenerateDebugBuildConfigTask(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void prepare(BuildType type) throws IOException {
    getModule().getJavaFiles();
    getModule().getKotlinFiles();
  }

  @Override
  public void run() throws IOException, CompilationFailedException {
    GenerateBuildConfig();
  }

  private void GenerateBuildConfig() throws IOException {
    Log.d(TAG, "Generating BuildConfig.java");
    String packageName = getApplicationId();

    File packageDir =
        new File(getModule().getBuildDirectory() + "/gen", packageName.replace('.', '/'));
    File buildConfigClass = new File(packageDir, "/BuildConfig.java");
    if (packageDir.exists()) {
    } else {
      packageDir.mkdirs();
    }
    File parentDir = buildConfigClass.getParentFile();
    if (!parentDir.exists()) {
      parentDir.mkdirs();
    }
    if (!buildConfigClass.exists() && !buildConfigClass.createNewFile()) {
      throw new IOException("Unable to generate BuildConfig.java");
    }

    String buildConfigString =
        "/**"
            + "\n"
            + "* Automatically generated file. DO NOT MODIFY"
            + "\n"
            + "*/"
            + "\n"
            + "package "
            + packageName
            + ";\n"
            + "\n"
            + "public final class BuildConfig {"
            + "\n"
            + "    public static final boolean DEBUG = "
            + "Boolean.parseBoolean(\"true\")"
            + ";\n"
            + "    public static final String APPLICATION_ID = "
            + "\"$package_name\"".replace("$package_name", packageName)
            + ";\n"
            + "    public static final String BUILD_TYPE = "
            + "\"debug\""
            + ";\n"
            + "    public static final int VERSION_CODE = "
            + getModule().getVersionCode()
            + ";\n"
            + "    public static final String VERSION_NAME = "
            + "\"$version_name\"".replace("$version_name", getModule().getVersionName())
            + ";\n"
            + "}\n";

    FileUtils.writeStringToFile(buildConfigClass, buildConfigString, Charset.defaultCharset());
  }

  public void GenerateBuildConfig(String packageName, File genDir) throws IOException {
    Log.d(TAG, "Generating BuildConfig.java");
    if (packageName == null) {
      throw new IOException("Unable to find namespace in build.gradle file");
    }
    File dir = new File(genDir, packageName.replace('.', '/'));
    File buildConfigClass = new File(dir, "/BuildConfig.java");
    if (genDir.exists() || dir.exists()) {
    } else {
      genDir.mkdirs();
      dir.mkdirs();
    }

    File parentDir = buildConfigClass.getParentFile();
    if (!parentDir.exists()) {
      parentDir.mkdirs();
    }

    if (!buildConfigClass.exists() && !buildConfigClass.createNewFile()) {
      throw new IOException("Unable to generate BuildConfig.java");
    }

    String buildConfigString =
        "/**"
            + "\n"
            + "* Automatically generated file. DO NOT MODIFY"
            + "\n"
            + "*/"
            + "\n"
            + "package "
            + packageName
            + ";\n"
            + "\n"
            + "public final class BuildConfig {"
            + "\n"
            + "    public static final boolean DEBUG = "
            + "Boolean.parseBoolean(\"true\")"
            + ";\n"
            + "    public static final String LIBRARY_PACKAGE_NAME = "
            + "\"$package_name\"".replace("$package_name", packageName)
            + ";\n"
            + "    public static final String BUILD_TYPE = "
            + "\"debug\""
            + ";\n"
            + "}\n";

    FileUtils.writeStringToFile(buildConfigClass, buildConfigString, Charset.defaultCharset());
  }

  private String getApplicationId() throws IOException {
    String packageName = getModule().getNameSpace();
    String content = parseString(getModule().getGradleFile());

    if (content != null) {
      if (content.contains("namespace") && !content.contains("applicationId")) {
        throw new IOException(
            "Unable to find applicationId in "
                + getModule().getRootFile().getName()
                + "/build.gradle file");

      } else if (content.contains("applicationId") && content.contains("namespace")) {
        return packageName;
      } else if (content.contains("applicationId") && !content.contains("namespace")) {
        packageName = getModule().getApplicationId();
      } else {
        throw new IOException(
            "Unable to find namespace or applicationId in "
                + getModule().getRootFile().getName()
                + "/build.gradle file");
      }
    } else {
      throw new IOException(
          "Unable to read " + getModule().getRootFile().getName() + "/build.gradle file");
    }
    return packageName;
  }

  private String parseString(File gradle) {
    if (gradle != null && gradle.exists()) {
      try {
        String readString = FileUtils.readFileToString(gradle, Charset.defaultCharset());
        if (readString != null && !readString.isEmpty()) {
          return readString;
        }
      } catch (IOException e) {
        // handle the exception here, if needed
      }
    }
    return null;
  }
}
